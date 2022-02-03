package de.dagere.peass.ci.logs;

import java.io.IOException;
import java.util.Set;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.measurement.MeasurementActionCreator;
import de.dagere.peass.ci.logs.rca.RCAActionCreator;
import de.dagere.peass.ci.logs.rts.RTSActionCreator;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.ci.process.RTSInfos;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class LogActionCreator {
   
   private final PeassProcessConfiguration peassConfig;
   private final Run<?, ?> run;
   private final LogFileReader reader;
   private final VisualizationFolderManager visualizationFolders;
   
   public LogActionCreator(final PeassProcessConfiguration peassConfig, final Run<?, ?> run, final VisualizationFolderManager visualizationFolders) {
      this.peassConfig = peassConfig;
      this.run = run;
      this.visualizationFolders = visualizationFolders;
      reader = new LogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
   }
   
   public RTSLogSummary createRTSActions(final RTSInfos staticChanges) throws IOException {
      RTSLogFileReader rtsReader = new RTSLogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
      RTSActionCreator rtsActionCreator = new RTSActionCreator(rtsReader, run, peassConfig.getMeasurementConfig(), peassConfig.getPattern());
      rtsActionCreator.createRTSActions(staticChanges);
      return rtsActionCreator.getLogSummary();
   }

   public void createMeasurementActions(final Set<TestCase> tests) throws IOException {
      MeasurementActionCreator measurementActionCreator = new MeasurementActionCreator(reader, run, peassConfig.getMeasurementConfig());
      measurementActionCreator.createMeasurementActions(tests);
   }
   
   public void createRCAActions() throws IOException {
      RCAActionCreator rcaActionCreator = new RCAActionCreator(reader, run, peassConfig.getMeasurementConfig());
      rcaActionCreator.createRCAActions();
   }
}
