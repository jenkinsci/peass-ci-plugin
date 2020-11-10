package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousExecutor;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcher;
import de.peass.measurement.rca.CauseSearcherComplete;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RCAExecutor{
   final MeasurementConfiguration config;
   final ContinuousExecutor executor;
   final ProjectChanges changes;
   public RCAExecutor(MeasurementConfiguration config, ContinuousExecutor executor, ProjectChanges changes) {
      this.config = config;
      this.executor = executor;
      this.changes = changes;
   }
   
   public void executeRCAs()
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      config.setVersion(executor.getLatestVersion());
      config.setVersionOld(executor.getVersionOld());
      MeasurementConfiguration currentConfig = new MeasurementConfiguration(config);
      

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            CauseSearchFolders folders = new CauseSearchFolders(executor.getProjectFolder());

            String onlyTestcaseName = (testcases.getKey().contains(".") ? testcases.getKey().substring(testcases.getKey().lastIndexOf('.') + 1) : testcases.getKey());
            final File expectedResultFile = new File(folders.getRcaTreeFolder(),
                  executor.getLatestVersion() + File.separator +
                        onlyTestcaseName + File.separator +
                        change.getMethod() + ".json");
            System.out.println("Testing " + expectedResultFile);
            if (!expectedResultFile.exists()) {
               executeRCA(currentConfig, executor, testcases, change);
            }
         }
      }
   }
   
   private void executeRCA(final MeasurementConfiguration config, final ContinuousExecutor executor, Entry<String, List<Change>> testcases, Change change)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final TestCase testCase = new TestCase(testcases.getKey() + "#" + change.getMethod());
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, true, true, 5.0, true, 0.01, false, true);
      config.setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(executor.getFolders().getProjectFolder());
      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(executor.getFolders().getProjectFolder(), config);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders);
      final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
      final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, config, alternateFolders);
      tester.search();
   }
}