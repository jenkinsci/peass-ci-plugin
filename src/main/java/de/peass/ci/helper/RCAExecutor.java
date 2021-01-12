package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.RCAStrategy;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.searcher.CauseSearcher;
import de.peass.measurement.rca.searcher.CauseSearcherComplete;
import de.peass.measurement.rca.searcher.LevelCauseSearcher;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RCAExecutor {
   final MeasurementConfiguration config;
   final ContinuousExecutor executor;
   final ProjectChanges changes;
   private RCAStrategy mode;
   private List<String> includes;

   public RCAExecutor(MeasurementConfiguration config, ContinuousExecutor executor, ProjectChanges changes, RCAStrategy mode, List<String> includes) {
      this.config = config;
      this.executor = executor;
      this.changes = changes;
      this.mode = mode;
      this.includes = includes;
   }

   public void executeRCAs()
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      config.setVersion(executor.getLatestVersion());
      config.setVersionOld(executor.getVersionOld());
      MeasurementConfiguration currentConfig = new MeasurementConfiguration(config);

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final TestCase testCase = new TestCase(testcases.getKey() + "#" + change.getMethod());
            boolean match = TestChooser.isTestIncluded(testCase, includes);
            if (match) {
               try {
                  analyseChange(currentConfig, testCase, change);
               } catch (Exception e) {
                  System.out.println("Was unable to analyze: " + change.getMethod());
                  e.printStackTrace();
               }
            }
         }
      }
   }

   private void analyseChange(MeasurementConfiguration currentConfig, TestCase testCase, Change change)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      CauseSearchFolders folders = new CauseSearchFolders(executor.getProjectFolder());

      String onlyTestcaseName = testCase.getShortClazz();
      final File expectedResultFile = new File(folders.getRcaTreeFolder(),
            executor.getLatestVersion() + File.separator +
                  onlyTestcaseName + File.separator +
                  change.getMethod() + ".json");
      System.out.println("Testing " + expectedResultFile);
      if (!expectedResultFile.exists()) {
         executeRCA(currentConfig, executor, testCase, change);
      }
   }

   private void executeRCA(final MeasurementConfiguration config, final ContinuousExecutor executor, TestCase testCase, Change change)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, true, true, 5.0, true, 0.01, false, true,
            RCAStrategy.COMPLETE);
      config.setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(executor.getFolders().getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders);
      
      CauseSearcher tester = RootCauseAnalysis.getCauseSeacher(config, causeSearcherConfig, alternateFolders, reader);
      tester.search();
   }
}