package de.dagere.peass.ci;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.serialization.TestMethodCallKeyDeserializer;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;

class TestStatisticsReading {

   @Test
   void testTestcaseNaming() throws Exception {
      File statisticsFile = new File("src/test/resources/statistics.json");

      Constants.OBJECTMAPPER.registerModules(new SimpleModule().addKeyDeserializer(TestMethodCall.class, new TestMethodCallKeyDeserializer()));

      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(statisticsFile, ProjectStatistics.class);

      Map<TestMethodCall, TestcaseStatistic> testcase = statistics.getStatistics().values().iterator().next();

      for (TestMethodCall test : testcase.keySet()) {
         assertThat(test.getClazz(), Matchers.not(Matchers.containsString(" ")));
      }
   }
}
