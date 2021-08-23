package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;

public class LogActionCreator {
   
   private final PeassProcessConfiguration peassConfig;
   
   public LogActionCreator(final PeassProcessConfiguration peassConfig) {
      this.peassConfig = peassConfig;
   }

   public void createActions(final File localWorkspace, final Run<?, ?> run, final ProjectStatistics statistics) throws IOException {
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      PeassFolders folders = visualizationFolders.getPeassFolders();
      LogFileReader creator = new LogFileReader(peassConfig.getMeasurementConfig());
      
      Map<TestCase, List<LogFiles>> logFiles = creator.readAllTestcases(folders, statistics);
      createLogActions(run, logFiles);
      
      run.addAction(new LogDisplayAction(logFiles, peassConfig.getMeasurementConfig().getVersion().substring(0,6), peassConfig.getMeasurementConfig().getVersionOld().substring(0,6)));
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
}
