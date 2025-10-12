package de.dagere.peass.ci.rts;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import hudson.model.Run;

class TestRTSVisualizationCreator {

   @Test
   void testEmptyCreation() throws Exception {
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(true, new MeasurementConfig(15), new TestSelectionConfig(1, false), null, 100, false, false, false,
            null);
      ResultsFolders localWorkspace = new ResultsFolders(new File("target/current"), "empty-test");

      writeEmptyDependencies(localWorkspace);
      writeEmptyExecutionData(localWorkspace);

      RTSVisualizationCreator creator = new RTSVisualizationCreator(localWorkspace, peassConfig);

      creator.visualize(Mockito.mock(Run.class), new RTSLogSummary(false, false, true, true, false, false));
   }

   private void writeEmptyExecutionData(final ResultsFolders localWorkspace) throws Exception {
      ExecutionData data = new ExecutionData();
      Constants.OBJECTMAPPER.writeValue(localWorkspace.getTraceTestSelectionFile(), data);
   }

   private void writeEmptyDependencies(final ResultsFolders localWorkspace) throws Exception {
      StaticTestSelection emptyDependencies = new StaticTestSelection();
      emptyDependencies.getInitialcommit().addDependency(new TestMethodCall("Test", "test"), new MethodCall("SomeCallee", "", "method"));
      Constants.OBJECTMAPPER.writeValue(localWorkspace.getStaticTestSelectionFile(), emptyDependencies);
   }
}
