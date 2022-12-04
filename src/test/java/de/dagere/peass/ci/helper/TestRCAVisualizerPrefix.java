package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.rca.RCAMetadata;
import de.dagere.peass.ci.rca.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.utils.Constants;
import hudson.model.Run;

public class TestRCAVisualizerPrefix {

   private static final File CHANGEFILE_FOLDER = new File("src/test/resources/changefiles");
   
   @Test
   public void testNoVisualizerPrefix() throws StreamReadException, DatabindException, IOException {
      File changefile = new File(CHANGEFILE_FOLDER, "changes_noPrefix.json");
      Run run = createChangefileActions(changefile);
      
      ArgumentCaptor<RCAVisualizationAction> captor = ArgumentCaptor.forClass(RCAVisualizationAction.class);
      Mockito.verify(run, Mockito.times(2)).addAction(captor.capture());
      
      List<RCAVisualizationAction> action = captor.getAllValues();
      Assert.assertEquals("myModule§de.package.TestClazzA_testSomething", action.get(0).getDisplayName());
      Assert.assertEquals("otherModule§net.package.TestClazzB_testSomethingElse", action.get(1).getDisplayName());
   }
   
   @Test
   public void testSimpleVisualizerPrefix() throws StreamReadException, DatabindException, IOException {
      File changefile = new File(CHANGEFILE_FOLDER, "changes_simplePrefix.json");
      Run run = createChangefileActions(changefile);
      
      ArgumentCaptor<RCAVisualizationAction> captor = ArgumentCaptor.forClass(RCAVisualizationAction.class);
      Mockito.verify(run, Mockito.times(2)).addAction(captor.capture());
      
      List<RCAVisualizationAction> action = captor.getAllValues();
      Assert.assertEquals("TestClazzA_testSomething", action.get(0).getDisplayName());
      Assert.assertEquals("TestClazzB_testSomethingElse", action.get(1).getDisplayName());
   }
   
   @Test
   public void testModuleVisualizerPrefix() throws StreamReadException, DatabindException, IOException {
      File changefile = new File(CHANGEFILE_FOLDER, "changes_modulePrefix.json");
      Run run = createChangefileActions(changefile);
      
      ArgumentCaptor<RCAVisualizationAction> captor = ArgumentCaptor.forClass(RCAVisualizationAction.class);
      Mockito.verify(run, Mockito.times(2)).addAction(captor.capture());
      
      List<RCAVisualizationAction> action = captor.getAllValues();
      Assert.assertEquals("de.package.TestClazzA_testSomething", action.get(0).getDisplayName());
      Assert.assertEquals("net.package.TestClazzB_testSomethingElse", action.get(1).getDisplayName());
   }

   private Run createChangefileActions(final File changefile) throws IOException, StreamReadException, DatabindException {
      ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changefile, ProjectChanges.class);
      Changes versionChanges = changes.getCommitChanges().values().iterator().next();

      String longestPrefix = RCAVisualizer.getLongestPrefix(versionChanges.getTestcaseChanges().keySet());
      
      Run run = Mockito.mock(Run.class);
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("000001");
      final File visualizationResultFolder = new File("target", "visualization_result");
      new File(visualizationResultFolder, "test").mkdirs();
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(visualizationResultFolder, "test", run);
      
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, measurementConfig, null, null, 0, false, false, false, null);
      RCAVisualizer rcaVisualizer = new RCAVisualizer(peassConfig, visualizationFolders, null, run);
      
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
//            final String name = testcases.getKey() + "_" + change.getMethod();
            RCAMetadata metadata = new RCAMetadata(change, testcases, peassConfig.getMeasurementConfig().getFixedCommitConfig(), visualizationResultFolder);
            rcaVisualizer.createRCAAction(new File("target/"), longestPrefix, testcases, change, metadata, changefile);
         }
      }
      return run;
   }
}
