package de.peass.ci.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import de.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;

public class TrendFileUtil {

   public static final String TREND_FILE_NAME = "trend.json";

   public static void persistTrend(final Run<?, ?> run, final File localWorkspace, final ProjectStatistics statistics)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File trendFile = new File(localWorkspace, TREND_FILE_NAME);
      BuildMeasurementValues values = getValues(trendFile);
      if (statistics.getStatistics().size() > 0) {
         if (values.getValues().size() == 0) {
            addFakePredecessorStatistics(run, statistics, values);
         }
         values.addMeasurement(statistics, run.getNumber());
         Constants.OBJECTMAPPER.writeValue(trendFile, values);
      }
   }

   private static void addFakePredecessorStatistics(final Run<?, ?> run, final ProjectStatistics statistics, final BuildMeasurementValues values) {
      final ProjectStatistics fakePredecessorStatistics = new ProjectStatistics();
      final Entry<String, Map<TestCase, TestcaseStatistic>> currentEntry = statistics.getStatistics().entrySet().iterator().next();
      final String version = currentEntry.getKey();
      for (Entry<TestCase, TestcaseStatistic> entry : currentEntry.getValue().entrySet()) {
         TestcaseStatistic predecessor = new TestcaseStatistic();
         predecessor.setCalls(entry.getValue().getCallsOld());
         predecessor.setMeanCurrent(entry.getValue().getMeanOld());
         predecessor.setDeviationCurrent(entry.getValue().getDeviationOld());
         predecessor.setVMs(entry.getValue().getVMs());
         fakePredecessorStatistics.addMeasurement(version + "~1", entry.getKey(), predecessor);
      }
      values.addMeasurement(fakePredecessorStatistics, run.getNumber() - 1);
   }

   public static BuildMeasurementValues readMeasurementValues(final File localWorkspace) throws JsonParseException, JsonMappingException, IOException, InterruptedException {
      final File trendFile = new File(localWorkspace, TREND_FILE_NAME);
      System.out.println(trendFile.getAbsolutePath() + " " + trendFile.exists());
      final BuildMeasurementValues values = getValues(trendFile);
      return values;
   }

   private static BuildMeasurementValues getValues(final File trendFile) throws IOException, JsonParseException, JsonMappingException {
      BuildMeasurementValues values;
      if (trendFile.exists()) {
         values = Constants.OBJECTMAPPER.readValue(trendFile, BuildMeasurementValues.class);
      } else {
         values = new BuildMeasurementValues();
      }
      return values;
   }
}
