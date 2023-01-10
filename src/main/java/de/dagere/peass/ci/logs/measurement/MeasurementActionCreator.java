package de.dagere.peass.ci.logs.measurement;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.ci.logs.LogUtil;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import hudson.model.Run;

public class MeasurementActionCreator {

   private static final Logger LOG = LogManager.getLogger(MeasurementActionCreator.class);

   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final PeassProcessConfiguration processConfig;
   private final MeasurementConfig measurementConfig;
   private final Pattern pattern;

   public MeasurementActionCreator(final LogFileReader reader, final Run<?, ?> run, final PeassProcessConfiguration processConfig) {
      this.reader = reader;
      this.run = run;
      this.processConfig = processConfig;
      this.measurementConfig = processConfig.getMeasurementConfig();
      this.pattern = processConfig.getPattern();
   }

   public void createMeasurementActions(final Set<TestMethodCall> tests) throws IOException {
      createOverallLogAction();

      Map<TestCase, List<LogFiles>> logFiles = reader.readAllTestcases(tests);
      createLogActions(run, logFiles);

      FixedCommitConfig fixedCommitConfig = measurementConfig.getFixedCommitConfig();
      String shortCommit = fixedCommitConfig.getCommit().substring(0, 6);
      String shortCommitOld = fixedCommitConfig.getCommitOld().substring(0, 6);
      LogOverviewAction logOverviewAction = new LogOverviewAction(IdHelper.getId(), logFiles, shortCommit, shortCommitOld, measurementConfig.getVms(),
            measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile());
      run.addAction(logOverviewAction);
   }

   private void createOverallLogAction() {
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         String measureLog = reader.getMeasureLog();
         String maskedLog = LogUtil.mask(measureLog, pattern);
         run.addAction(new InternalLogAction(IdHelper.getId(), "measurementLog", "Measurement Log", maskedLog));
      }
   }

   private void createLogActions(final Run<?, ?> run, final Map<TestCase, List<LogFiles>> logFiles) throws IOException {
      for (Map.Entry<TestCase, List<LogFiles>> entry : logFiles.entrySet()) {
         LOG.debug("Creating {} log actions for {}", entry.getValue().size(), entry.getKey());
         TestCase testcase = entry.getKey();
         int vmId = 0;
         for (LogFiles files : entry.getValue()) {
            String logData = processConfig.getFileText(files.getCurrent());
            run.addAction(new LogAction(IdHelper.getId(), testcase, vmId, measurementConfig.getFixedCommitConfig().getCommit(), logData));
            String logDataOld = processConfig.getFileText(files.getPredecessor());
            run.addAction(new LogAction(IdHelper.getId(), testcase, vmId, measurementConfig.getFixedCommitConfig().getCommitOld(), logDataOld));
            vmId++;
         }
      }
   }
}
