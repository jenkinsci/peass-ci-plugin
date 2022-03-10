package de.dagere.peass.ci.logs.measurement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.ci.logs.LogUtil;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class MeasurementActionCreator {

   private static final Logger LOG = LogManager.getLogger(MeasurementActionCreator.class);

   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfig measurementConfig;
   private final Pattern pattern;

   public MeasurementActionCreator(final LogFileReader reader, final Run<?, ?> run, final MeasurementConfig measurementConfig, final Pattern pattern) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
      this.pattern = pattern;
   }

   public void createMeasurementActions(final Set<TestCase> tests) throws IOException {
      createOverallLogAction();

      Map<TestCase, List<LogFiles>> logFiles = reader.readAllTestcases(tests);
      createLogActions(run, logFiles);

      ExecutionConfig executionConfig = measurementConfig.getExecutionConfig();
      String shortVersion = executionConfig.getVersion().substring(0, 6);
      String shortVersionOld = executionConfig.getVersionOld().substring(0, 6);
      LogOverviewAction logOverviewAction = new LogOverviewAction(logFiles, shortVersion, shortVersionOld, measurementConfig.getVms(),
            executionConfig.isRedirectSubprocessOutputToFile());
      run.addAction(logOverviewAction);
   }

   private void createOverallLogAction() {
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         String measureLog = reader.getMeasureLog();
         String maskedLog = LogUtil.mask(measureLog, pattern);
         run.addAction(new InternalLogAction("measurementLog", "Measurement Log", maskedLog));
      }
   }

   private void createLogActions(final Run<?, ?> run, final Map<TestCase, List<LogFiles>> logFiles) throws IOException {
      for (Map.Entry<TestCase, List<LogFiles>> entry : logFiles.entrySet()) {
         LOG.debug("Creating {} log actions for {}", entry.getValue().size(), entry.getKey());
         TestCase testcase = entry.getKey();
         int vmId = 0;
         for (LogFiles files : entry.getValue()) {
            String logData = FileUtils.readFileToString(files.getCurrent(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, measurementConfig.getExecutionConfig().getVersion(), logData));
            String logDataOld = FileUtils.readFileToString(files.getPredecessor(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, measurementConfig.getExecutionConfig().getVersionOld(), logDataOld));
            vmId++;
         }
      }
   }
}
