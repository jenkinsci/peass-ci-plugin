package de.dagere.peass.ci.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.module.SimpleModule;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.serialization.TestMethodCallKeyDeserializer;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

class TestLogFileReader {

   private static final String VERSION_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   private static final String VERSION = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private final File localFolder = new File("target/" + MeasureVersionBuilder.PEASS_FOLDER_NAME);
   private final File testFolder = new File(localFolder, "current_peass");

   private static final File RESOURCES_FOLDER = new File("src/test/resources/");

   @BeforeEach
   void setUp() throws Exception {
      SimpleModule methodDeserializer = new SimpleModule().addKeyDeserializer(TestMethodCall.class, new TestMethodCallKeyDeserializer());
      Constants.OBJECTMAPPER.registerModules(methodDeserializer);

      File source = new File(RESOURCES_FOLDER, "demo-results-logs/demo-vis2_peass");
      if (localFolder.exists()) {
         FileUtils.deleteDirectory(localFolder);
      }
      if (!localFolder.exists()) {
         localFolder.mkdirs();
      }

      FileUtils.copyDirectory(source, testFolder);

      File demoResultMeasurements = new File(RESOURCES_FOLDER,
            "demo-results/histogram/measurement_a23e385264c31def8dcda86c3cf64faa698c62d8_33ce17c04b5218c25c40137d4d09f40fbb3e4f0f");
      FileUtils.copyDirectory(demoResultMeasurements, new File(localFolder, "measurement_a23e385264c31def8dcda86c3cf64faa698c62d8_33ce17c04b5218c25c40137d4d09f40fbb3e4f0f"));

      ResultsFolders folders = new ResultsFolders(localFolder, "demo-vis2");
      File measurementLogFile = folders.getMeasurementLogFile(VERSION, VERSION_OLD);
      FileUtils.write(measurementLogFile, "This is a measurement log test", StandardCharsets.UTF_8);

   }

   @Test
   void testReading() throws Exception {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, VERSION, VERSION_OLD);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, peassDemoConfig, null, null, 5, false, false, false, null);

      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      LogFileReader reader = new LogFileReader(visualizationFolders, peassConfig);
      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File("src/test/resources/demo-results-logs/statistics.json"), ProjectStatistics.class);
      Map<TestCase, List<LogFiles>> testcases = reader.readAllTestcases(statistics.getStatistics().get(VERSION).keySet());

      assertEquals(1, testcases.size());
      TestMethodCall test = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod2");
      List<LogFiles> logFiles = testcases.get(test);
      assertEquals(2, logFiles.size());

      String measureLog = reader.getMeasureLog();
      assertEquals("This is a measurement log test", measureLog);
   }

   @Test
   void testReadingIterationChanged() throws Exception {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, VERSION, VERSION_OLD);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, peassDemoConfig, null, null, 5, false, false, false, null);

      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      LogFileReader reader = new LogFileReader(visualizationFolders, peassConfig);
      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File("src/test/resources/demo-results-logs/statistics.json"), ProjectStatistics.class);
      Map<TestCase, List<LogFiles>> testcases = reader.readAllTestcases(statistics.getStatistics().get(VERSION).keySet());

      assertEquals(1, testcases.size());
      TestMethodCall test = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod2");
      List<LogFiles> logFiles = testcases.get(test);
      assertEquals(2, logFiles.size());

      assertFalse(logFiles.get(0).isCurrentSuccess());
      assertTrue(logFiles.get(0).isPredecessorSuccess());

      assertTrue(logFiles.get(1).isCurrentSuccess());
      assertTrue(logFiles.get(1).isPredecessorSuccess());
   }
}
