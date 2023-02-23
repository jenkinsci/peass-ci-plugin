package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.FileFilter;
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
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.ci.rca.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.utils.Constants;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

public class TestRCAVisualizer {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder(new File("target"));

   @Test
   public void testHTMLGeneration() throws IOException {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);

      initFolders();
      final File visualizationResultFolder = new File(folder.getRoot(), "visualization_result");
      
      final Run run = mockRun(visualizationResultFolder);
      visualizeRCAForTest(changes, run);

      testCorrectResult(run, visualizationResultFolder);
   }
   
   @Test
   public void testMissingJson() throws Exception {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);

      final File visualizationResultFolder = new File(folder.getRoot(), "visualization_result");
      
      CauseSearchFolders folders = initFolders();
      File rcaTreeFile = folders.getRcaTreeFile("b02c92af73e3297be617f4c973a7a63fb603565b", new TestMethodCall("de.test.CalleeTest", "onlyCallMethod1"));
      rcaTreeFile.delete();
      
      final Run run = mockRun(visualizationResultFolder);
      visualizeRCAForTest(changes, run);

      Mockito.verify(run, Mockito.times(1)).setResult(Result.UNSTABLE);
   }

   private void visualizeRCAForTest(ProjectChanges changes, final Run run) throws IOException {
      // Calls the RCAVisualizer, which should be tested
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("b02c92af73e3297be617f4c973a7a63fb603565b");
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, measurementConfig, null, null, 0, false, false, false, null);
      
      new File(folder.getRoot(), "project").mkdirs();
      
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(folder.getRoot(), "project", run);
      visualizationFolders.getPropertyFolder().mkdir();
      RCAVisualizer visualizer = new RCAVisualizer(peassConfig, visualizationFolders, changes, run);
      visualizer.visualizeRCA();
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
      File jsFile = resultFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(File pathname) {
            return !pathname.isDirectory();
         }
      } )[0];

      MatcherAssert.assertThat(jsFile.getName(), Matchers.endsWith(".js"));
   }

   private CauseSearchFolders initFolders() throws IOException {
      final File projectFolder = new File(folder.getRoot(), "project");
      final CauseSearchFolders peassFolders = new CauseSearchFolders(projectFolder);
      FileUtils.copyDirectory(new File("src/test/resources/demo-results/rca/rca"), peassFolders.getRcaTreeFolder().getParentFile());
      
      return peassFolders;
   }
}
