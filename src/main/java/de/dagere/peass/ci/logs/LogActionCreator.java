package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.ci.logs.rca.RCALogOverviewAction;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;

public class LogActionCreator {
   
   private final PeassProcessConfiguration peassConfig;
   private final Run<?, ?> run;
   
   public LogActionCreator(final PeassProcessConfiguration peassConfig, final Run<?, ?> run) {
      this.peassConfig = peassConfig;
      this.run = run;
   }

   public void createActions(final File localWorkspace, final ProjectStatistics statistics) throws IOException {
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      LogFileReader reader = new LogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
      
      Map<TestCase, List<LogFiles>> logFiles = reader.readAllTestcases(statistics);
      createLogActions(run, logFiles);
      
      String rtsLog = reader.getRTSLog();
      run.addAction(new InternalLogAction("rtsLog", "Regression Test Selection Log", rtsLog));
      
      String measureLog = reader.getMeasureLog();
      run.addAction(new InternalLogAction("measurementLog", "Measurement Log", measureLog));
      
      LogOverviewAction logOverviewAction = new LogOverviewAction(logFiles, peassConfig.getMeasurementConfig().getVersion().substring(0,6), peassConfig.getMeasurementConfig().getVersionOld().substring(0,6));
      run.addAction(logOverviewAction);
   }
   
   private void createLogActions(final Run<?, ?> run, final Map<TestCase, List<LogFiles>> logFiles) throws IOException {
      for (Map.Entry<TestCase, List<LogFiles>> entry : logFiles.entrySet()) {
         TestCase testcase = entry.getKey();
         int vmId = 0;
         for (LogFiles files : entry.getValue()) {
            String logData = FileUtils.readFileToString(files.getCurrent(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, peassConfig.getMeasurementConfig().getVersion(), logData));
            String logDataOld = FileUtils.readFileToString(files.getPredecessor(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, peassConfig.getMeasurementConfig().getVersionOld(), logDataOld));
            vmId++;
         }
      }
   }
   
   public void createRCAActions(final File localWorkspace) {
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      LogFileReader reader = new LogFileReader(visualizationFolders, peassConfig.getMeasurementConfig());
      String rcaLog = reader.getRCALog();
      run.addAction(new InternalLogAction("rcaLog", "RCA Log", rcaLog));
      
      Map<TestCase, List<RCALevel>> testLevelMap = reader.getRCATestcases();
      
      RCALogOverviewAction rcaOverviewAction = new RCALogOverviewAction(testLevelMap, peassConfig.getMeasurementConfig().getVersion().substring(0,6), peassConfig.getMeasurementConfig().getVersionOld().substring(0,6));
      run.addAction(rcaOverviewAction);
   }
}
