package de.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.RTSLogFileReader;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TraceWriter;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class TestRTSLogFileReader {

   private static final String VERSION_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   private static final String VERSION = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private final File localFolder = new File("target/peass-data");
   private final File testFolder = new File(localFolder, "current_peass");

   private final TestCase TEST1 = new TestCase("de.test.CalleeTest#onlyCallMethod1");
   private final TestCase TEST2 = new TestCase("de.test.CalleeTest#onlyCallMethod2");

   @BeforeEach
   public void init() throws IOException {
      File source = new File("src/test/resources/demo-results-logs/demo-vis2_peass");
      if (localFolder.exists()) {
         FileUtils.deleteDirectory(localFolder);
      }
      if (!localFolder.exists()) {
         localFolder.mkdirs();
      }

      FileUtils.copyDirectory(source, testFolder);

      ResultsFolders folders = initExampleLogFile();

      initExampleViewFile(folders);
   }

   private ResultsFolders initExampleLogFile() throws IOException {
      ResultsFolders folders = new ResultsFolders(localFolder, "demo-vis2");
      File rtsLogFile = folders.getDependencyLogFile(VERSION, VERSION_OLD);
      FileUtils.write(rtsLogFile, "This is a rts log test", StandardCharsets.UTF_8);
      return folders;
   }

   private void initExampleViewFile(final ResultsFolders folders) throws IOException {
      File viewMethodDir = folders.getViewMethodDir(VERSION_OLD, TEST1);
      File methodFile = new File(viewMethodDir, TraceWriter.getShortVersion(VERSION_OLD));
      FileUtils.write(methodFile, "This is a rts diff", StandardCharsets.UTF_8);
   }

   @Test
   public void testReading() throws JsonParseException, JsonMappingException, IOException {
      RTSLogFileReader reader = initializeReader();
      Map<String, File> testcases = reader.findProcessSuccessRuns();

      Assert.assertEquals(1, testcases.size());
      File testRunningFile = testcases.get("a23e385264c31def8dcda86c3cf64faa698c62d8");
      Assert.assertTrue(testRunningFile.exists());

      Assert.assertTrue(reader.isLogsExisting());

      Map<TestCase, RTSLogData> rtsVmRuns = reader.getRtsVmRuns("a23e385264c31def8dcda86c3cf64faa698c62d8");
      Assert.assertEquals(2, rtsVmRuns.size());

      File dataFile1 = rtsVmRuns.get(TEST1).getMethodFile();
      Assert.assertTrue(dataFile1.exists());
      RTSLogData logDataTest2 = rtsVmRuns.get(TEST2);
      File dataFile2 = logDataTest2.getMethodFile();
      Assert.assertTrue(dataFile2.exists());
      Assert.assertFalse(logDataTest2.isSuccess());

      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = reader.getRtsVmRuns("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f");
      Assert.assertEquals(2, rtsVmRunsPredecessor.size());
      RTSLogData rtsLogData = rtsVmRunsPredecessor.get(TEST1);
      Assert.assertEquals("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f", rtsLogData.getVersion());
      Assert.assertTrue(rtsLogData.isSuccess());

      String rtsLog = reader.getRTSLog();
      Assert.assertEquals("This is a rts log test", rtsLog);
   }

   private RTSLogFileReader initializeReader() {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, VERSION, VERSION_OLD);

      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      RTSLogFileReader reader = new RTSLogFileReader(visualizationFolders, peassDemoConfig);
      return reader;
   }

   @Test
   public void testReadingOnlyOverviewExists() throws IOException {
      FileUtils.deleteDirectory(testFolder);

      RTSLogFileReader reader = initializeReader();
      Assert.assertTrue(reader.isLogsExisting());
   }
}
