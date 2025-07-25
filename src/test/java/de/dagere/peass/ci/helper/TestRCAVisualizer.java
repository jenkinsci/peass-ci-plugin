package de.dagere.peass.ci.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.ci.rca.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.utils.Constants;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

class TestRCAVisualizer {

   @TempDir
   private File folder;

   @Test
   void testHTMLGeneration() throws Exception {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);

      initFolders();
      final File visualizationResultFolder = new File(folder, "visualization_result");

      final Run run = mockRun(visualizationResultFolder);
      visualizeRCAForTest(changes, run);

      testCorrectResult(run, visualizationResultFolder);
   }

   @Test
   void testMissingJson() throws Exception {
      final File testChangeFile = new File("src/test/resources/demo-results/rca/changes.json");
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(testChangeFile, ProjectChanges.class);

      final File visualizationResultFolder = new File(folder, "visualization_result");

      CauseSearchFolders folders = initFolders();
      File rcaTreeFile = folders.getRcaTreeFile("b02c92af73e3297be617f4c973a7a63fb603565b", new TestMethodCall("de.test.CalleeTest", "onlyCallMethod1"));
      rcaTreeFile.delete();

      final Run run = mockRun(visualizationResultFolder);
      visualizeRCAForTest(changes, run);

      Mockito.verify(run, Mockito.times(1)).setResult(Result.UNSTABLE);
   }

   private void visualizeRCAForTest(ProjectChanges changes, final Run run) throws Exception {
      // Calls the RCAVisualizer, which should be tested
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("b02c92af73e3297be617f4c973a7a63fb603565b");
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, measurementConfig, null, null, 0, false, false, false, null);

      new File(folder, "project").mkdirs();

      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(folder, "project", run);
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
      assertEquals("CalleeTest_onlyCallMethod1", argument.getValue().getDisplayName());

      File resultFolder = visualizationResultFolder.listFiles()[0];
      File jsFile = resultFolder.listFiles(pathname -> !pathname.isDirectory())[0];

      assertThat(jsFile.getName(), Matchers.endsWith(".js"));
   }

   private CauseSearchFolders initFolders() throws Exception {
      final File projectFolder = new File(folder, "project");
      final CauseSearchFolders peassFolders = new CauseSearchFolders(projectFolder);
      FileUtils.copyDirectory(new File("src/test/resources/demo-results/rca/rca"), peassFolders.getRcaTreeFolder().getParentFile());

      return peassFolders;
   }
}
