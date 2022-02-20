package de.dagere.peass.ci.logs.rca;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.ci.logs.LogUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class RCAActionCreator {
   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfig measurementConfig;
   private final Pattern pattern;

   public RCAActionCreator(final LogFileReader reader, final Run<?, ?> run, final MeasurementConfig measurementConfig, final Pattern pattern) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
      this.pattern = pattern;
   }

   public void createRCAActions() throws IOException {
      createOverallActionLog();

      Map<TestCase, List<RCALevel>> testLevelMap = createRCALogActions(reader);

      RCALogOverviewAction rcaOverviewAction = new RCALogOverviewAction(testLevelMap, measurementConfig.getExecutionConfig().getVersion().substring(0, 6),
            measurementConfig.getExecutionConfig().getVersionOld().substring(0, 6), measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile());
      run.addAction(rcaOverviewAction);
   }

   private void createOverallActionLog() {
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         String rcaLog = reader.getRCALog();
         String maskedLog = LogUtil.mask(rcaLog, pattern);
         run.addAction(new InternalLogAction("rcaLog", "RCA Log", maskedLog));
      }
   }

   private Map<TestCase, List<RCALevel>> createRCALogActions(final LogFileReader reader) throws IOException {
      Map<TestCase, List<RCALevel>> testLevelMap = reader.getRCATestcases();
      for (Map.Entry<TestCase, List<RCALevel>> testcase : testLevelMap.entrySet()) {
         int levelId = 0;
         for (RCALevel level : testcase.getValue()) {
            createLevelLogAction(testcase, levelId, level);
            levelId++;
         }
      }
      return testLevelMap;
   }

   private void createLevelLogAction(final Map.Entry<TestCase, List<RCALevel>> testcase, final int levelId, final RCALevel level) throws IOException {
      int vmId = 0;
      for (LogFiles files : level.getLogFiles()) {
         createVMLogActions(testcase, levelId, vmId, files);
         vmId++;
      }
   }

   private void createVMLogActions(final Map.Entry<TestCase, List<RCALevel>> testcase, final int levelId, final int vmId, final LogFiles files) throws IOException {
      addLog(testcase, levelId, vmId, files.getCurrent(), measurementConfig.getExecutionConfig().getVersion());
      addLog(testcase, levelId, vmId, files.getPredecessor(), measurementConfig.getExecutionConfig().getVersionOld());
   }

   private void addLog(final Map.Entry<TestCase, List<RCALevel>> testcase, final int levelId, final int vmId, final File logFile, final String version) throws IOException {
      String logData;
      if (logFile.exists()) {
         logData = FileUtils.readFileToString(logFile, StandardCharsets.UTF_8);
      } else {
         logData = "Log file could not be found";
      }
      
      run.addAction(new RCALogAction(testcase.getKey(), vmId, levelId, version, logData));
   }
}
