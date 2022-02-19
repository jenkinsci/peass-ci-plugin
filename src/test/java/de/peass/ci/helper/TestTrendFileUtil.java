package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.persistence.BuildMeasurementValues;
import de.dagere.peass.ci.persistence.TestMeasurementValues;
import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import hudson.model.Run;

/**
 * Tests the behaviour of trendfile creation: For the first time, the measurement values of the version and its predecessor is added. Afterwards, only the current version is added (otherwise, the trendfile would contain 
 * very frequent changes)
 * @author DaGeRe
 *
 */
public class TestTrendFileUtil {

   private static final File LOCAL_WORKSPACE = new File("target");

   private static final int VERSION_INDEX = 15;

   @Before
   public void cleanTrendfile() {
      File trendFile = new File(LOCAL_WORKSPACE, TrendFileUtil.TREND_FILE_NAME);
      trendFile.delete();
   }

   @Test
   public void testFirstAddition() throws JsonParseException, JsonMappingException, JsonGenerationException, IOException, InterruptedException {
      Run run = Mockito.mock(Run.class);

      ProjectStatistics simpleStatistics = buildStatistics();

      Mockito.when(run.getNumber()).thenReturn(VERSION_INDEX);
      TrendFileUtil.persistTrend(run, LOCAL_WORKSPACE, simpleStatistics);

      checkFirstAddition();

      Mockito.when(run.getNumber()).thenReturn(VERSION_INDEX + 1);
      TrendFileUtil.persistTrend(run, LOCAL_WORKSPACE, simpleStatistics);

      BuildMeasurementValues values = TrendFileUtil.readMeasurementValues(LOCAL_WORKSPACE);
      TestMeasurementValues testcaseValues = values.getValues().get("DemoTest#methodA");
      Assert.assertEquals(testcaseValues.getStatistics().size(), 3);
      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX + 1).getMeanOld(), 1, 0.01);
      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX + 1).getMeanCurrent(), 2, 0.01);
   }

   private void checkFirstAddition() throws JsonParseException, JsonMappingException, IOException, InterruptedException {
      BuildMeasurementValues values = TrendFileUtil.readMeasurementValues(LOCAL_WORKSPACE);

      MatcherAssert.assertThat(values.getValues().keySet(), Matchers.contains("DemoTest#methodA", "DemoTest#methodB"));
      TestMeasurementValues testcaseValues = values.getValues().get("DemoTest#methodA");
      Assert.assertEquals(testcaseValues.getStatistics().size(), 2);

      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX - 1).getMeanOld(), 0, 0.01);
      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX - 1).getMeanCurrent(), 1, 0.01);

      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX).getMeanOld(), 1, 0.01);
      Assert.assertEquals(testcaseValues.getStatistics().get(VERSION_INDEX).getMeanCurrent(), 2, 0.01);
   }

   private ProjectStatistics buildStatistics() {
      ProjectStatistics simpleStatistics = new ProjectStatistics();
      simpleStatistics.addMeasurement("000001", new TestCase("DemoTest", "methodA"), new TestcaseStatistic(1, 2, 0.1, 0.2, 15, -5, true, 100, 100));
      simpleStatistics.addMeasurement("000001", new TestCase("DemoTest", "methodB"), new TestcaseStatistic(3, 4, 0.2, 0.3, 20, -5, true, 100, 100));
      return simpleStatistics;
   }
}
