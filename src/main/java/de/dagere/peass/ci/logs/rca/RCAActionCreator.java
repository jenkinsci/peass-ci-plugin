package de.dagere.peass.ci.logs.rca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.logs.InternalLogAction;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;

public class RCAActionCreator {
   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfiguration measurementConfig;

   public RCAActionCreator(final LogFileReader reader, final Run<?, ?> run, final MeasurementConfiguration measurementConfig) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
   }

   public void createRCAActions() throws IOException {
      createOverallActionLog();

      Map<TestCase, List<RCALevel>> testLevelMap = createRCALogActions(reader);

      RCALogOverviewAction rcaOverviewAction = new RCALogOverviewAction(testLevelMap, measurementConfig.getVersion().substring(0, 6),
            measurementConfig.getVersionOld().substring(0, 6));
      run.addAction(rcaOverviewAction);
   }

   private void createOverallActionLog() {
      if (measurementConfig.isRedirectSubprocessOutputToFile()) {
         String rcaLog = reader.getRCALog();
         run.addAction(new InternalLogAction("rcaLog", "RCA Log", rcaLog));
      }
   }

   private Map<TestCase, List<RCALevel>> createRCALogActions(final LogFileReader reader) throws IOException {
      Map<TestCase, List<RCALevel>> testLevelMap = reader.getRCATestcases();
      for (Map.Entry<TestCase, List<RCALevel>> testcase : testLevelMap.entrySet()) {
         int levelId = 0;
         for (RCALevel level : testcase.getValue()) {
            int vmId = 0;
            for (LogFiles files : level.getLogFiles()) {
               String logData = FileUtils.readFileToString(files.getCurrent(), StandardCharsets.UTF_8);
               run.addAction(new RCALogAction(testcase.getKey(), vmId, levelId, measurementConfig.getVersion(), logData));
               String logDataOld = FileUtils.readFileToString(files.getPredecessor(), StandardCharsets.UTF_8);
               run.addAction(new RCALogAction(testcase.getKey(), vmId, levelId, measurementConfig.getVersionOld(), logDataOld));
               vmId++;
            }
            levelId++;
         }
      }
      return testLevelMap;
   }
}
