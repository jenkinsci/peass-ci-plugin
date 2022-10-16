package de.dagere.peass.ci.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import hudson.model.Run;

public class TrendFileUtil {

   public static final String TREND_FILE_NAME = "trend.json";

   public static void persistTrend(final Run<?, ?> run, final File localWorkspace, final ProjectStatistics statistics) {
      File trendFile = new File(localWorkspace, TREND_FILE_NAME);
      BuildMeasurementValues values = getValues(trendFile);
      if (statistics.getStatistics().size() > 0) {
         if (values.getValues().size() == 0) {
            addFakePredecessorStatistics(run, statistics, values);
         }
         values.addMeasurement(statistics, run.getNumber());
         try {
            Constants.OBJECTMAPPER.writeValue(trendFile, values);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
   }

   private static void addFakePredecessorStatistics(final Run<?, ?> run, final ProjectStatistics statistics, final BuildMeasurementValues values) {
      final ProjectStatistics fakePredecessorStatistics = new ProjectStatistics();
      final Entry<String, Map<TestMethodCall, TestcaseStatistic>> currentEntry = statistics.getStatistics().entrySet().iterator().next();
      final String commit = currentEntry.getKey();
      for (Entry<TestMethodCall, TestcaseStatistic> entry : currentEntry.getValue().entrySet()) {
         TestcaseStatistic predecessor = new TestcaseStatistic();
         predecessor.setCalls(entry.getValue().getCallsOld());
         predecessor.setMeanCurrent(entry.getValue().getMeanOld());
         predecessor.setDeviationCurrent(entry.getValue().getDeviationOld());
         predecessor.setVMs(entry.getValue().getVMs());
         fakePredecessorStatistics.addMeasurement(commit + "~1", entry.getKey(), predecessor);
      }
      values.addMeasurement(fakePredecessorStatistics, run.getNumber() - 1);
   }

   public static BuildMeasurementValues readMeasurementValues(final File localWorkspace) throws JsonParseException, JsonMappingException, IOException, InterruptedException {
      final File trendFile = new File(localWorkspace, TREND_FILE_NAME);
      System.out.println(trendFile.getAbsolutePath() + " " + trendFile.exists());
      final BuildMeasurementValues values = getValues(trendFile);
      return values;
   }

   private static BuildMeasurementValues getValues(final File trendFile) {
      BuildMeasurementValues values;
      if (trendFile.exists()) {
         try {
            values = Constants.OBJECTMAPPER.readValue(trendFile, BuildMeasurementValues.class);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         values = new BuildMeasurementValues();
      }
      return values;
   }
}
