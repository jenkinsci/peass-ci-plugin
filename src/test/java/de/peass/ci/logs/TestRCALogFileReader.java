package de.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class TestRCALogFileReader {

   private static final String VERSION_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   private static final String VERSION = "a23e385264c31def8dcda86c3cf64faa698c62d8";
   private final File localFolder = new File("target/peass-data");
   private final File testFolder = new File(localFolder, "current_peass");

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

      ResultsFolders folders = new ResultsFolders(localFolder, "demo-vis2");
      File rcaLogFile = folders.getRCALogFile(VERSION, VERSION_OLD);
      FileUtils.write(rcaLogFile, "This is a rca log test", StandardCharsets.UTF_8);
   }

   @Test
   public void testReading() throws JsonParseException, JsonMappingException, IOException {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, VERSION, VERSION_OLD);

      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getPeassRCAFolders()).thenReturn(new CauseSearchFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      LogFileReader reader = new LogFileReader(visualizationFolders, peassDemoConfig);

      TestCase test = new TestCase("de.test.CalleeTest#onlyCallMethod2");
      Map<TestCase, List<RCALevel>> rcaTestcases = reader.getRCATestcases();
      List<RCALevel> levels = rcaTestcases.get(test);
      Assert.assertEquals(1, levels.size());
      MatcherAssert.assertThat(levels.get(0).getLogFiles(), Matchers.hasSize(3));

      String rtsLog = reader.getRCALog();
      Assert.assertEquals("This is a rca log test", rtsLog);

   }
}
