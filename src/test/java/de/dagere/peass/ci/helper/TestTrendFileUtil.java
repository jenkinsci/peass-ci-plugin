package de.dagere.peass.ci.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.persistence.BuildMeasurementValues;
import de.dagere.peass.ci.persistence.TestMeasurementValues;
import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import hudson.model.Run;

/**
 * Tests the behaviour of trendfile creation: For the first time, the measurement values of the commit and its predecessor is added. Afterwards, only the current version is added (otherwise, the trendfile would contain
 * very frequent changes)
 * @author DaGeRe
 *
 */
class TestTrendFileUtil {

   private static final File LOCAL_WORKSPACE = new File("target");

   private static final int COMMIT_INDEX = 15;

   @BeforeEach
   void setUp() {
      File trendFile = new File(LOCAL_WORKSPACE, TrendFileUtil.TREND_FILE_NAME);
      trendFile.delete();
   }

   @Test
   void testFirstAddition() throws Exception {
      Run run = Mockito.mock(Run.class);

      ProjectStatistics simpleStatistics = buildStatistics();

      Mockito.when(run.getNumber()).thenReturn(COMMIT_INDEX);
      TrendFileUtil.persistTrend(run, LOCAL_WORKSPACE, simpleStatistics);

      checkFirstAddition();

      Mockito.when(run.getNumber()).thenReturn(COMMIT_INDEX + 1);
      TrendFileUtil.persistTrend(run, LOCAL_WORKSPACE, simpleStatistics);

      BuildMeasurementValues values = TrendFileUtil.readMeasurementValues(LOCAL_WORKSPACE);
      TestMeasurementValues testcaseValues = values.getValues().get("DemoTest#methodA");
      assertEquals(3, testcaseValues.getStatistics().size());
      assertEquals(1, testcaseValues.getStatistics().get(COMMIT_INDEX + 1).getMeanOld(), 0.01);
      assertEquals(2, testcaseValues.getStatistics().get(COMMIT_INDEX + 1).getMeanCurrent(), 0.01);
   }

   private void checkFirstAddition() throws Exception {
      BuildMeasurementValues values = TrendFileUtil.readMeasurementValues(LOCAL_WORKSPACE);

      assertThat(values.getValues().keySet(), Matchers.contains("DemoTest#methodA", "DemoTest#methodB"));
      TestMeasurementValues testcaseValues = values.getValues().get("DemoTest#methodA");
      assertEquals(2, testcaseValues.getStatistics().size());

      assertEquals(0, testcaseValues.getStatistics().get(COMMIT_INDEX - 1).getMeanOld(), 0.01);
      assertEquals(1, testcaseValues.getStatistics().get(COMMIT_INDEX - 1).getMeanCurrent(), 0.01);

      assertEquals(1, testcaseValues.getStatistics().get(COMMIT_INDEX).getMeanOld(), 0.01);
      assertEquals(2, testcaseValues.getStatistics().get(COMMIT_INDEX).getMeanCurrent(), 0.01);
   }

   private ProjectStatistics buildStatistics() {
      ProjectStatistics simpleStatistics = new ProjectStatistics();
      simpleStatistics.addMeasurement("000001", new TestMethodCall("DemoTest", "methodA"), new TestcaseStatistic(1, 2, 0.1, 0.2, 15, -5, 0.1, true, 100, 100));
      simpleStatistics.addMeasurement("000001", new TestMethodCall("DemoTest", "methodB"), new TestcaseStatistic(3, 4, 0.2, 0.3, 20, -5, 0.1, true, 100, 100));
      return simpleStatistics;
   }
}
