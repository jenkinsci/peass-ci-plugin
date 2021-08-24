package de.dagere.peass.ci.logs.measurement;

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
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;

public class MeasurementActionCreator {
   private final LogFileReader reader;
   private final Run<?, ?> run;
   private final MeasurementConfiguration measurementConfig;

   public MeasurementActionCreator(final LogFileReader reader, final Run<?, ?> run, final MeasurementConfiguration measurementConfig) {
      this.reader = reader;
      this.run = run;
      this.measurementConfig = measurementConfig;
   }

   public void createMeasurementActions(final ProjectStatistics statistics) throws IOException {
      Map<TestCase, List<LogFiles>> logFiles = reader.readAllTestcases(statistics);
      createLogActions(run, logFiles);

      String measureLog = reader.getMeasureLog();
      run.addAction(new InternalLogAction("measurementLog", "Measurement Log", measureLog));

      LogOverviewAction logOverviewAction = new LogOverviewAction(logFiles, measurementConfig.getVersion().substring(0, 6), measurementConfig.getVersionOld().substring(0, 6));
      run.addAction(logOverviewAction);
   }

   private void createLogActions(final Run<?, ?> run, final Map<TestCase, List<LogFiles>> logFiles) throws IOException {
      for (Map.Entry<TestCase, List<LogFiles>> entry : logFiles.entrySet()) {
         TestCase testcase = entry.getKey();
         int vmId = 0;
         for (LogFiles files : entry.getValue()) {
            String logData = FileUtils.readFileToString(files.getCurrent(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, measurementConfig.getVersion(), logData));
            String logDataOld = FileUtils.readFileToString(files.getPredecessor(), StandardCharsets.UTF_8);
            run.addAction(new LogAction(testcase, vmId, measurementConfig.getVersionOld(), logDataOld));
            vmId++;
         }
      }
   }
}
