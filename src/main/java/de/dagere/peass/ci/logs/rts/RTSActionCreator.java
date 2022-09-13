package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogUtil;
import de.dagere.peass.ci.logs.RTSLogFileReader;
import de.dagere.peass.ci.process.RTSInfos;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import hudson.model.Run;

public class RTSActionCreator {

   private static final Logger LOG = LogManager.getLogger(RTSActionCreator.class);

   private final RTSLogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfig measurementConfig;
   private final Map<String, Boolean> processSuccessRunSucceeded = new HashMap<>();
   private RTSLogSummary logSummary;
   private final Pattern pattern;

   public RTSActionCreator(final RTSLogFileReader reader, final Run<?, ?> run, final MeasurementConfig measurementConfig, final Pattern pattern) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
      this.pattern = pattern;
   }

   public void createRTSActions(final RTSInfos staticChanges) throws IOException {
      if (reader.isLogsExisting()) {
         createOverallLogAction();
      } else {
         LOG.info("No RTS Actions existing; not creating regression test selection actions.");
      }

      Map<String, File> processSuccessRuns = createProcessSuccessRunsActions();

      Map<TestCase, RTSLogData> rtsVmRuns = createVersionRTSData(measurementConfig.getFixedCommitConfig().getCommit(), staticChanges.getIgnoredTestsCurrent());
      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = createVersionRTSData(measurementConfig.getFixedCommitConfig().getCommitOld(), staticChanges.getIgnoredTestsPredecessor());

      logSummary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      createOverviewAction(processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor, staticChanges);
   }

   private void createOverviewAction(final Map<String, File> processSuccessRuns, final Map<TestCase, RTSLogData> rtsVmRuns, final Map<TestCase, RTSLogData> rtsVmRunsPredecessor,
         final RTSInfos rtsInfos) {
      RTSLogOverviewAction overviewAction = new RTSLogOverviewAction(IdHelper.getId(), processSuccessRuns, rtsVmRuns, rtsVmRunsPredecessor,
            processSuccessRunSucceeded, measurementConfig.getFixedCommitConfig().getCommit(), measurementConfig.getFixedCommitConfig().getCommitOld(),
            measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile());
      overviewAction.setStaticChanges(rtsInfos.isStaticChanges());
      overviewAction.setStaticallySelectedTests(rtsInfos.isStaticallySelectedTests());
      run.addAction(overviewAction);
   }

   private void createOverallLogAction() {
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         String rtsLog = reader.getRTSLog();
         String maskedLog = LogUtil.mask(rtsLog, pattern);
         run.addAction(new InternalLogAction(IdHelper.getId(), "rtsLog", "Regression Test Selection Log", maskedLog));

         String sourceReadingLog = reader.getSourceReadingLog();
         run.addAction(new InternalLogAction(IdHelper.getId(), "sourceLog", "Source Reading Log", sourceReadingLog));
      }
   }

   private Map<String, File> createProcessSuccessRunsActions() throws IOException {
      Map<String, File> processSuccessRuns = reader.findProcessSuccessRuns();
      for (Map.Entry<String, File> processSuccessRun : processSuccessRuns.entrySet()) {
         String logData = FileUtils.readFileToString(processSuccessRun.getValue(), StandardCharsets.UTF_8);
         /**
          * This is not exactly what is required here - if process sucess runs are realy executed for both versions (which currently does not happen by default), than the value
          * should be obtained for both versions
          */
         processSuccessRunSucceeded.put(processSuccessRun.getKey(), reader.isVersionRunWasSuccess());
         ProcessSuccessLogAction processSuccessAction = new ProcessSuccessLogAction(IdHelper.getId(), "processSuccessRun_" + processSuccessRun.getKey(), logData,
               processSuccessRun.getKey());
         run.addAction(processSuccessAction);
      }
      return processSuccessRuns;
   }

   private Map<TestCase, RTSLogData> createVersionRTSData(final String commit, final TestSet ignoredTests) throws IOException {
      Map<TestCase, RTSLogData> rtsVmRuns = reader.getRtsVmRuns(commit, ignoredTests);
      LOG.info("RTS Runs: {}", rtsVmRuns.size());
      for (Map.Entry<TestCase, RTSLogData> rtsLogData : rtsVmRuns.entrySet()) {
         String methodLogData = getLogData(rtsLogData.getValue().getMethodFile());
         String cleanLogData = getLogData(rtsLogData.getValue().getCleanFile());
         RTSLogAction logAction = new RTSLogAction(IdHelper.getId(), rtsLogData.getValue().getVersion(), rtsLogData.getKey(), cleanLogData, methodLogData);
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

   public RTSLogSummary getLogSummary() {
      return logSummary;
   }
}
