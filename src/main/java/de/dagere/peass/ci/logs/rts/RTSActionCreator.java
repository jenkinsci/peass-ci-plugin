package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.RTSLogFileReader;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class RTSActionCreator {

   private static final Logger LOG = LogManager.getLogger(RTSActionCreator.class);

   private final RTSLogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfiguration measurementConfig;

   public RTSActionCreator(final RTSLogFileReader reader, final Run<?, ?> run, final MeasurementConfiguration measurementConfig) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
   }

   public void createRTSActions() throws IOException {
      if (reader.isLogsExisting()) {
         createOverallLogAction();

         Map<String, File> processSuccessRuns = createProcessSuccessRunsActions();

         Map<TestCase, RTSLogData> rtsVmRuns = createVersionRTSData(measurementConfig.getVersion());
         Map<TestCase, RTSLogData> rtsVmRunsPredecessor = createVersionRTSData(measurementConfig.getVersionOld());

         createOverviewAction(processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor);
      } else {
         LOG.info("No RTS Actions existing; not creating regression test selection actions.");
      }
   }

   private void createOverviewAction(final Map<String, File> processSuccessRuns, final Map<TestCase, RTSLogData> rtsVmRuns, final Map<TestCase, RTSLogData> rtsVmRunsPredecessor) {
      RTSLogOverviewAction overviewAction = new RTSLogOverviewAction(processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor);
      run.addAction(overviewAction);
   }

   private void createOverallLogAction() {
      if (measurementConfig.isRedirectSubprocessOutputToFile()) {
         String rtsLog = reader.getRTSLog();
         run.addAction(new InternalLogAction("rtsLog", "Regression Test Selection Log", rtsLog));
      }
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
         String methodLogData = getLogData(rtsLogData.getValue().getMethodFile());
         String cleanLogData = getLogData(rtsLogData.getValue().getCleanFile());
         RTSLogAction logAction = new RTSLogAction(rtsLogData.getValue().getVersion(), rtsLogData.getKey(), cleanLogData, methodLogData);
         run.addAction(logAction);
      }
      return rtsVmRuns;
   }

   private String getLogData(final File methodFile) throws IOException {
      String methodLogData;
      if (methodFile.exists()) {
         methodLogData = FileUtils.readFileToString(methodFile, StandardCharsets.UTF_8);
      } else {
         methodLogData = "Log could not be loaded";
      }
      return methodLogData;
   }
}
