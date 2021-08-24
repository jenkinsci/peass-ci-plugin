package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class RTSActionCreator {
   
   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfiguration measurementConfig;

   public RTSActionCreator(final LogFileReader reader, final Run<?, ?> run, final MeasurementConfiguration measurementConfig) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
   }

   public void createRTSActions() throws IOException {
      createOverallLogAction();
      
      Map<String, File> processSuccessRuns = createProcessSuccessRunsActions();
      
      Map<TestCase, RTSLogData> rtsVmRuns = createVersionRTSData(measurementConfig.getVersion());
      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = createVersionRTSData(measurementConfig.getVersion());
      
      createOverviewAction(processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor);
   }

   private void createOverviewAction(Map<String, File> processSuccessRuns, Map<TestCase, RTSLogData> rtsVmRuns, Map<TestCase, RTSLogData> rtsVmRunsPredecessor) {
      RTSLogOverviewAction overviewAction = new RTSLogOverviewAction(processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor);
      run.addAction(overviewAction);
   }

   private void createOverallLogAction() {
      String rtsLog = reader.getRTSLog();
      run.addAction(new InternalLogAction("rtsLog", "Regression Test Selection Log", rtsLog));
   }

   private Map<String, File> createProcessSuccessRunsActions() throws IOException {
      Map<String, File> processSuccessRuns = reader.findProcessSuccessRuns();
      for (Map.Entry<String, File> processSuccessRun : processSuccessRuns.entrySet()) {
         String logData = FileUtils.readFileToString(processSuccessRun.getValue(), StandardCharsets.UTF_8);
         run.addAction(new ProcessSuccessLogAction("processSuccessRun_" + processSuccessRun.getKey(), logData, processSuccessRun.getKey()));
      }
      return processSuccessRuns;
   }

   private Map<TestCase, RTSLogData> createVersionRTSData(final String version) throws IOException {
      Map<TestCase, RTSLogData> rtsVmRuns = reader.getRtsVmRuns(version);
      for (Map.Entry<TestCase, RTSLogData> rtsLogData : rtsVmRuns.entrySet()) {
         String methodLogData = FileUtils.readFileToString(rtsLogData.getValue().getMethodFile(), StandardCharsets.UTF_8);
         String cleanLogData = FileUtils.readFileToString(rtsLogData.getValue().getCleanFile(), StandardCharsets.UTF_8);
         RTSLogAction logAction = new RTSLogAction(rtsLogData.getValue().getVersion(), rtsLogData.getKey(), cleanLogData, methodLogData);
         run.addAction(logAction);
      }
      return rtsVmRuns;
   }
}
