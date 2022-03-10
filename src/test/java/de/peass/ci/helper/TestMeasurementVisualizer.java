package de.peass.ci.helper;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.dagere.peass.ci.MeasurementVisualizationAction;
import de.dagere.peass.ci.helper.DefaultMeasurementVisualizer;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import hudson.model.Run;

public class TestMeasurementVisualizer {

   @Test
   public void testParameterizedVisualization() {
      HashSet<String> tests = new LinkedHashSet<>();
      tests.add("de.dagere.peass.ExampleTest#test(JUNIT_PARAMETERIZED-0)");
      tests.add("de.dagere.peass.ExampleTest#test(JUNIT_PARAMETERIZED-1)");

      File exampleDataFolder = new File(
            "src/test/resources/demo-results-measurements/measurement_a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b_49f75e8877c2e9b7cf6b56087121a35fdd73ff8b/");
      Run run = Mockito.mock(Run.class);
      VisualizationFolderManager visualizationFolderManager = Mockito.mock(VisualizationFolderManager.class);
      DefaultMeasurementVisualizer visualizer = new DefaultMeasurementVisualizer(exampleDataFolder, "a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b", run, visualizationFolderManager,
            tests);

      visualizer.visualizeMeasurements();

      ArgumentCaptor<MeasurementVisualizationAction> captor = ArgumentCaptor.forClass(MeasurementVisualizationAction.class);
      Mockito.verify(run, Mockito.times(2)).addAction(captor.capture());

      MeasurementVisualizationAction action1 = captor.getAllValues().get(0);
      Assert.assertEquals(action1.getDisplayName(), "measurement_ExampleTest_test(JUNIT_PARAMETERIZED-1)");

      MeasurementVisualizationAction action2 = captor.getAllValues().get(1);
      Assert.assertEquals(action2.getDisplayName(), "measurement_ExampleTest_test(JUNIT_PARAMETERIZED-0)");

      Map<String, TestcaseStatistic> noWarmupStatistics = visualizer.getNoWarmupStatistics();
      Assert.assertTrue(noWarmupStatistics.containsKey("de.dagere.peass.ExampleTest#test(JUNIT_PARAMETERIZED-0)"));
      Assert.assertTrue(noWarmupStatistics.containsKey("de.dagere.peass.ExampleTest#test(JUNIT_PARAMETERIZED-1)"));
   }
}
