package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.measurement.MeasurementActionCreator;
import de.dagere.peass.ci.logs.rca.RCAActionCreator;
import de.dagere.peass.ci.logs.rts.RTSActionCreator;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;

public class LogActionCreator {
   
   private final PeassProcessConfiguration peassConfig;
   private final Run<?, ?> run;
   private final LogFileReader reader;
   private final VisualizationFolderManager visualizationFolders;
   
   public LogActionCreator(final PeassProcessConfiguration peassConfig, final Run<?, ?> run, final File localWorkspace) {
      this.peassConfig = peassConfig;
      this.run = run;
      visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      reader = new LogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
   }
   
   public void createRTSActions() throws IOException {
      RTSLogFileReader rtsReader = new RTSLogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
      RTSActionCreator rtsActionCreator = new RTSActionCreator(rtsReader, run, peassConfig.getMeasurementConfig());
      rtsActionCreator.createRTSActions();
   }

   public void createMeasurementActions(final ProjectStatistics statistics) throws IOException {
      MeasurementActionCreator measurementActionCreator = new MeasurementActionCreator(reader, run, peassConfig.getMeasurementConfig());
      measurementActionCreator.createMeasurementActions(statistics);
   }
   
   public void createRCAActions() throws IOException {
      RCAActionCreator rcaActionCreator = new RCAActionCreator(reader, run, peassConfig.getMeasurementConfig());
      rcaActionCreator.createRCAActions();
   }
}
