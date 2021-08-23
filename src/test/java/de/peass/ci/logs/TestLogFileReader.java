package de.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import de.dagere.peass.utils.Constants;

public class TestLogFileReader {
   
   private static final String VERSION_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   private static final String VERSION = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private final File localFolder = new File("target/peass-data");
   private final File testFolder = new File(localFolder, "current_peass");
   
   @BeforeEach
   public void init() throws IOException {
      File source = new File("src/test/resources/demo-results-logs/demo-vis2_peass");
      if (testFolder.exists()) {
         FileUtils.deleteDirectory(testFolder);
      }
      if (!localFolder.exists()) {
         localFolder.mkdirs();
      }
      
      FileUtils.copyDirectory(source, testFolder);
      
      ResultsFolders folders = new ResultsFolders(localFolder, "demo-vis2");
      File rcaLogFile = folders.getRCALogFile(VERSION, VERSION_OLD);
      FileUtils.write(rcaLogFile, "This is a rca log test", StandardCharsets.UTF_8);
   }
   
   @Test
   public void testReading() throws JsonParseException, JsonMappingException, IOException {
      MeasurementConfiguration peassDemoConfig = new MeasurementConfiguration(2, VERSION, VERSION_OLD);
      
      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      LogFileReader reader = new LogFileReader(visualizationFolders, peassDemoConfig);
      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File("src/test/resources/demo-results-logs/statistics.json"), ProjectStatistics.class);
      Map<TestCase, List<LogFiles>> testcases = reader.readAllTestcases(statistics);
      
      Assert.assertEquals(1, testcases.size());
      TestCase test = new TestCase("de.test.CalleeTest#onlyCallMethod2");
      List<LogFiles> logFiles = testcases.get(test);
      Assert.assertEquals(2, logFiles.size());
      
      String rtsLog = reader.getRTSLog();
      Assert.assertEquals("This is a rts log test", rtsLog);
      
      String measureLog = reader.getMeasureLog();
      Assert.assertEquals("This is a measurement log test", measureLog);
   }
}
