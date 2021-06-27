package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.helper.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.utils.Constants;
import de.peass.ci.RCAVisualizationAction;
import hudson.model.Job;
import hudson.model.Run;

public class TestRCAVisualizer {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testHTMLGeneration() throws Exception {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);

      initFolders();
      final File visualizationResultFolder = new File(folder.getRoot(), "visualization_result");
      
      final Run run = mockRun(visualizationResultFolder);

      // Calls the RCAVisualizer, which should be tested
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      measurementConfig.setVersion("b02c92af73e3297be617f4c973a7a63fb603565b");
      RCAVisualizer visualizer = new RCAVisualizer(measurementConfig, folder.getRoot(), changes, run);
      visualizer.visualizeRCA();

      testCorrectResult(run, visualizationResultFolder);
   }

   private Run mockRun(final File visualizationResultFolder) {
      final Run run = Mockito.mock(Run.class);
      Mockito.when(run.getRootDir()).thenReturn(visualizationResultFolder);
      Job job = Mockito.mock(Job.class, Mockito.RETURNS_MOCKS);
      Mockito.when(job.getFullDisplayName()).thenReturn("project");
      Mockito.when(run.getParent()).thenReturn(job);
      return run;
   }

   private void testCorrectResult(final Run run, final File visualizationResultFolder) {
      ArgumentCaptor<RCAVisualizationAction> argument = ArgumentCaptor.forClass(RCAVisualizationAction.class);
      Mockito.verify(run).addAction(argument.capture());
      Assert.assertEquals("CalleeTest_onlyCallMethod1", argument.getValue().getDisplayName());

      File resultFolder = visualizationResultFolder.listFiles()[0];
      File htmlFile = resultFolder.listFiles()[0];

      MatcherAssert.assertThat(htmlFile.getName(), Matchers.endsWith(".js"));
   }

   private void initFolders() throws IOException {
      final File projectFolder = new File(folder.getRoot(), "project");
      final CauseSearchFolders peassFolders = new CauseSearchFolders(projectFolder);
      FileUtils.copyDirectory(new File("src/test/resources/demo-results/rca/rca_data"), peassFolders.getRcaTreeFolder());
   }
}
