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

import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.RCAVisualizationAction;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.utils.Constants;
import hudson.model.Run;

public class TestRCAVisualizer {
   
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();
   
   @Test
   public void testHTMLGeneration() throws Exception {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);
      
//      final ContinuousExecutor continuousExecutorMock = mockExecutor();
      
      final Run run = Mockito.mock(Run.class);
      final File visualizationResultFolder = new File(folder.getRoot(), "visualization_result");
      Mockito.when(run.getRootDir()).thenReturn(visualizationResultFolder);
      
      //Calls the RCAVisualizer, which should be tested
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      measurementConfig.setVersion("b02c92af73e3297be617f4c973a7a63fb603565b");
      RCAVisualizer visualizer = new RCAVisualizer(measurementConfig, folder.getRoot(), changes, run);
      visualizer.visualizeRCA();
      
      testCorrectResult(run, visualizationResultFolder);
   }

   private void testCorrectResult(final Run run, final File visualizationResultFolder) {
      ArgumentCaptor<RCAVisualizationAction> argument = ArgumentCaptor.forClass(RCAVisualizationAction.class);
      Mockito.verify(run).addAction(argument.capture());
      Assert.assertEquals("CalleeTest_onlyCallMethod1", argument.getValue().getDisplayName());
      
      File resultFolder = visualizationResultFolder.listFiles()[0];
      File htmlFile = resultFolder.listFiles()[0];
      
      MatcherAssert.assertThat(htmlFile.getName(), Matchers.endsWith(".js"));
   }

   private void mockExecutor() throws IOException {
//      final ContinuousExecutor continuousExecutorMock = Mockito.mock(ContinuousExecutor.class);
      
//      Mockito.when(continuousExecutorMock.getLocalFolder((File) Mockito.any())).thenReturn(folder.getRoot());
      final File projectFolder = new File(folder.getRoot(), "project");
      final CauseSearchFolders peassFolders = new CauseSearchFolders(projectFolder);
      FileUtils.copyDirectory(new File("src/test/resources/demo-results/rca/rca_data"), peassFolders.getRcaTreeFolder());
//      Mockito.when(continuousExecutorMock.getFolders()).thenReturn(peassFolders);
//      Mockito.when(continuousExecutorMock.getLatestVersion()).thenReturn("b02c92af73e3297be617f4c973a7a63fb603565b");
//      return continuousExecutorMock;
   }
}
