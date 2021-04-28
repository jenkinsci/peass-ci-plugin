package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.RootCauseAnalysis;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.searcher.CauseSearcher;
import de.dagere.peass.utils.Constants;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RCAExecutor {

   private static final Logger LOG = LogManager.getLogger(RCAExecutor.class);

   private final MeasurementConfiguration config;
   private final File projectFolder;
   private final ProjectChanges changes;
   private final CauseSearcherConfig causeConfig;
   private final EnvironmentVariables env;

   public RCAExecutor(final MeasurementConfiguration config, final File workspaceFolder, final ProjectChanges changes, final CauseSearcherConfig causeConfig,
         final EnvironmentVariables env) {
      this.config = config;
      this.projectFolder = workspaceFolder;
      this.changes = changes;
      this.causeConfig = causeConfig;
      this.env = env;
   }

   public void executeRCAs()
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      Changes versionChanges = changes.getVersion(config.getVersion());

      boolean needsRCA = checkNeedsRCA(versionChanges);

      if (needsRCA) {
         LOG.info("At least one testcase was not successfully executed in the last build for the current version - executing RCA");
         saveOldPeassFolder();

         MeasurementConfiguration currentConfig = new MeasurementConfiguration(config);

         for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
            for (Change change : testcases.getValue()) {
               final TestCase testCase = new TestCase(testcases.getKey(), change.getMethod());
               boolean match = NonIncludedTestRemover.isTestIncluded(testCase, config.getIncludes());
               if (match) {
                  try {
                     analyseChange(currentConfig, testCase);
                  } catch (Exception e) {
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

   private boolean checkNeedsRCA(final Changes versionChanges) throws IOException, JsonParseException, JsonMappingException {
      boolean needsRCA = false;
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final TestCase testCase = new TestCase(testcases.getKey(), change.getMethod());
            boolean match = NonIncludedTestRemover.isTestIncluded(testCase, config.getIncludes());
            if (match) {
               final File expectedResultFile = getExpectedRCAFile(testCase);
               if (!expectedResultFile.exists()) {
                  needsRCA = true;
               } else {
                  CauseSearchData lastData = Constants.OBJECTMAPPER.readValue(expectedResultFile, CauseSearchData.class);
                  if (lastData.getMeasurementConfig().getVersion().equals(config.getVersion())
                        && lastData.getMeasurementConfig().getVersionOld().equals(config.getVersionOld())) {
                     LOG.debug("Found version {} vs {} of testcase {}", config.getVersion(), config.getVersionOld(), testCase);
                     LOG.debug("RCA-file: {}", expectedResultFile.getAbsolutePath());
                  } else {
                     LOG.debug("Did not find version {} vs {} of testcase {}", config.getVersion(), config.getVersionOld(), testCase);
                     needsRCA = true;
                  }
               }
            }
         }
      }
      return needsRCA;
   }

   private void analyseChange(final MeasurementConfiguration currentConfig, final TestCase testCase)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final File expectedResultFile = getExpectedRCAFile(testCase);
      LOG.info("Testing {}", expectedResultFile);
      if (!expectedResultFile.exists()) {
         LOG.debug("Needs execution");
         executeRCA(currentConfig, testCase);
      }
   }

   private File getExpectedRCAFile(final TestCase testCase) {
      CauseSearchFolders folders = new CauseSearchFolders(projectFolder);
      final File expectedResultFile = new File(folders.getRcaTreeFolder(config.getVersion(), testCase),
            testCase.getMethod() + ".json");
      return expectedResultFile;
   }

   private void executeRCA(final MeasurementConfiguration config, final TestCase testCase)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, causeConfig);
      config.setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(projectFolder);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders, env);

      CauseSearcher tester = RootCauseAnalysis.getCauseSeacher(config, causeSearcherConfig, alternateFolders, reader);
      tester.search();
   }

   private void saveOldPeassFolder() {
      final File oldPeassFolder = PeASSFolders.getPeassFolder(projectFolder);
      if (oldPeassFolder.exists()) {
         int i = 0;
         File destFolder = new File(oldPeassFolder.getParentFile(), "oldPeassFolder_" + i);
         while (destFolder.exists()) {
            i++;
            destFolder = new File(oldPeassFolder.getParentFile(), "oldPeassFolder_" + i);
         }
         LOG.debug("Moving Peass folder {} to {}", oldPeassFolder, destFolder.getAbsolutePath());
         boolean success = oldPeassFolder.renameTo(destFolder);
         LOG.debug("Success: {}", success);
      } else {
         LOG.debug("Folder {} does not exist", oldPeassFolder.getAbsolutePath());
      }
   }
}