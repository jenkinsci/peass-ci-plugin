package de.peass.ci.persistence;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.ci.ContinuousExecutor;
import de.peass.measurement.analysis.ProjectStatistics;
import de.peass.utils.Constants;
import hudson.model.Run;

public class TrendFileUtil {
   public static void persistTrend(final Run<?, ?> run, final ContinuousExecutor executor, final ProjectStatistics statistics)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File trendFile = new File(executor.getLocalFolder(), "trend.json");
      BuildMeasurementValues values = getValues(trendFile);
      values.addMeasurement(statistics, run.getNumber());
      Constants.OBJECTMAPPER.writeValue(trendFile, values);
   }

   public static BuildMeasurementValues readMeasurementValues(final File projectFolder) throws JsonParseException, JsonMappingException, IOException, InterruptedException {
      final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, null, 1, true);
      File trendFile = new File(executor.getLocalFolder(), "trend.json");
      BuildMeasurementValues values = getValues(trendFile);
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
