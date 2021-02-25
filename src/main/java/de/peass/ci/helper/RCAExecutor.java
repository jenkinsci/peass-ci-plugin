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

import de.peass.RootCauseAnalysis;
import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousExecutor;
import de.peass.ci.TestChooser;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.RCAStrategy;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RCAExecutor {

   private static final Logger LOG = LogManager.getLogger(RCAExecutor.class);

   private final MeasurementConfiguration config;
   private final ContinuousExecutor executor;
   private final ProjectChanges changes;
   private final RCAStrategy rcaStrategy;
   private final List<String> includes;

   public RCAExecutor(final MeasurementConfiguration config, final ContinuousExecutor executor, final ProjectChanges changes, final RCAStrategy rcaStrategy,
         final List<String> includes) {
      this.config = config;
      this.executor = executor;
      this.changes = changes;
      this.rcaStrategy = rcaStrategy;
      this.includes = includes;
   }

   public void executeRCAs()
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      Changes versionChanges = changes.getVersion(executor.getLatestVersion());

      boolean needsRCA = checkNeedsRCA(versionChanges);

      if (needsRCA) {
         LOG.info("At least one testcase was not successfully executed in the last build for the current version - executing RCA");
         saveOldPeassFolder(executor);

         config.setVersion(executor.getLatestVersion());
         config.setVersionOld(executor.getVersionOld());
         MeasurementConfiguration currentConfig = new MeasurementConfiguration(config);

         for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
            for (Change change : testcases.getValue()) {
               final TestCase testCase = new TestCase(testcases.getKey(), change.getMethod());
               boolean match = TestChooser.isTestIncluded(testCase, includes);
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

   private boolean checkNeedsRCA(Changes versionChanges) throws IOException, JsonParseException, JsonMappingException {
      boolean needsRCA = false;
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final TestCase testCase = new TestCase(testcases.getKey(), change.getMethod());
            boolean match = TestChooser.isTestIncluded(testCase, includes);
            if (match) {
               final File expectedResultFile = getExpectedRCAFile(testCase);
               if (!expectedResultFile.exists()) {
                  needsRCA = true;
               } else {
                  CauseSearchData lastData = Constants.OBJECTMAPPER.readValue(expectedResultFile, CauseSearchData.class);
                  if (lastData.getMeasurementConfig().getVersion().equals(config.getVersion())
                        && lastData.getMeasurementConfig().getVersionOld().equals(config.getVersionOld())) {
                     LOG.debug("Found version {} vs {} of testcase {}", config.getVersion(), config.getVersionOld(), testCase);
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
         executeRCA(currentConfig, executor, testCase);
      }
   }

   private File getExpectedRCAFile(final TestCase testCase) {
      CauseSearchFolders folders = new CauseSearchFolders(executor.getProjectFolder());
      final File expectedResultFile = new File(folders.getRcaTreeFolder(executor.getLatestVersion(), testCase),
            testCase.getMethod() + ".json");
      return expectedResultFile;
   }

   private void executeRCA(final MeasurementConfiguration config, final ContinuousExecutor executor, final TestCase testCase)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, true, true, 5.0, true, 0.01, false, true,
            rcaStrategy);
      config.setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(executor.getFolders().getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders);

      CauseSearcher tester = RootCauseAnalysis.getCauseSeacher(config, causeSearcherConfig, alternateFolders, reader);
      tester.search();
   }

   private void saveOldPeassFolder(final ContinuousExecutor executor) {
      final File oldPeassFolder = executor.getFolders().getPeassFolder();
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