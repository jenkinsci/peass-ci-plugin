package de.dagere.peass.ci.rca;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.SearchCauseStarter;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RCAExecutor {

   private static final Logger LOG = LogManager.getLogger(RCAExecutor.class);

   private final MeasurementConfig config;
   private final File projectFolder;
   private final ProjectChanges changes;
   private final CauseSearcherConfig causeConfig;
   private final EnvironmentVariables env;
   private final CommitComparatorInstance comparator;
   private List<TestCase> failedTests = new LinkedList<>();

   public RCAExecutor(final MeasurementConfig config, final File workspaceFolder, final ProjectChanges changes, final CauseSearcherConfig causeConfig,
         final EnvironmentVariables env) {
      this.config = config;
      this.projectFolder = workspaceFolder;
      this.changes = changes;
      this.causeConfig = causeConfig;
      this.env = env;
      this.comparator = new CommitComparatorInstance(Arrays.asList(config.getFixedCommitConfig().getCommitOld(), config.getFixedCommitConfig().getCommit()));
   }

   public void executeRCAs() throws IOException {
      Changes commitChanges = changes.getCommitChanges(config.getFixedCommitConfig().getCommit());

      boolean needsRCA = checkNeedsRCA(commitChanges);

      if (needsRCA) {
         LOG.info("At least one testcase was not successfully executed in the last build for the current version - executing RCA");
         // saveOldPeassFolder();

         MeasurementConfig currentConfig = new MeasurementConfig(config);
         currentConfig.setDirectlyMeasureKieker(false);

         for (Entry<String, List<Change>> testcases : commitChanges.getTestcaseChanges().entrySet()) {
            for (Change change : testcases.getValue()) {
               final TestMethodCall testCase;
               String testClazzName = testcases.getKey();
               if (testClazzName.contains(ChangedEntity.MODULE_SEPARATOR)) {
                  int moduleSeparatorIndex = testClazzName.indexOf(ChangedEntity.MODULE_SEPARATOR);
                  String module = testClazzName.substring(0, moduleSeparatorIndex);
                  String testclazz = testClazzName.substring(moduleSeparatorIndex + 1, testClazzName.length());
                  testCase = new TestMethodCall(testclazz, change.getMethod(), module, change.getParams());
               } else {
                  testCase = new TestMethodCall(testClazzName, change.getMethod(), "", change.getParams());
               }
               boolean match = NonIncludedTestRemover.isTestIncluded(testCase, config.getExecutionConfig());
               if (match) {
                  try {
                     analyseChange(currentConfig, testCase);
                  } catch (Exception e) {
                     failedTests.add(testCase);
                     System.out.println("Was unable to analyze: " + change.getMethod());
                     e.printStackTrace();
                  }
               } else {
                  LOG.info("Skipping not included test: {}", testCase);
               }
            }
         }
      }

   }

   private boolean checkNeedsRCA(final Changes commitChanges) throws IOException {
      boolean needsRCA = false;
      FixedCommitConfig commitConfig = config.getFixedCommitConfig();
      for (Entry<String, List<Change>> testcases : commitChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final TestMethodCall testCase = TestMethodCall.createFromClassString(testcases.getKey(), change.getMethod());
            boolean match = NonIncludedTestRemover.isTestIncluded(testCase, config.getExecutionConfig());
            if (match) {
               final File expectedResultFile = getExpectedRCAFile(testCase);
               if (!expectedResultFile.exists()) {
                  needsRCA = true;
                  LOG.debug("Did not find commit {} vs {} of testcase {}", commitConfig.getCommit(), commitConfig.getCommitOld(), testCase);
               } else {
                  CauseSearchData lastData = Constants.OBJECTMAPPER.readValue(expectedResultFile, CauseSearchData.class);
                  String commitInData = lastData.getMeasurementConfig().getFixedCommitConfig().getCommit();
                  String commitOldInData = lastData.getMeasurementConfig().getFixedCommitConfig().getCommitOld();
                  if (commitInData.equals(commitConfig.getCommit())
                        && commitOldInData.equals(commitConfig.getCommitOld())) {
                     LOG.debug("Found commit {} vs {} of testcase {}", commitConfig.getCommit(), commitConfig.getCommitOld(), testCase);
                     LOG.debug("RCA-file: {}", expectedResultFile.getAbsolutePath());
                  } else {
                     LOG.debug("Did not find commit {} vs {} of testcase {}", commitConfig.getCommit(), commitConfig.getCommitOld(), testCase);
                     needsRCA = true;
                  }
               }
            }
         }
      }
      return needsRCA;
   }

   private void analyseChange(final MeasurementConfig currentConfig, final TestMethodCall testCase)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException {
      final File expectedResultFile = getExpectedRCAFile(testCase);
      LOG.info("Testing {}", expectedResultFile);
      if (!expectedResultFile.exists()) {
         LOG.debug("Needs execution");
         executeRCA(currentConfig, testCase);
      }
   }

   private File getExpectedRCAFile(final TestMethodCall testCase) {
      CauseSearchFolders folders = new CauseSearchFolders(projectFolder);
      final File expectedResultFile = new File(folders.getRcaTreeFolder(config.getFixedCommitConfig().getCommit(), testCase),
            testCase.getMethodWithParams() + ".json");
      return expectedResultFile;
   }

   private void executeRCA(final MeasurementConfig config, final TestMethodCall testCase)
         throws IOException, InterruptedException {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, causeConfig);
      config.getKiekerConfig().setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(projectFolder);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders, env);

      CauseSearcher tester = SearchCauseStarter.getCauseSeacher(config, causeSearcherConfig, alternateFolders, reader, comparator);
      tester.search();
   }

   public List<TestCase> getFailedTests() {
      return failedTests;
   }
}