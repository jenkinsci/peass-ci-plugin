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

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.LogFileReader;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class TestRCALogFileReader {

   private final File localFolder = new File("target/" + MeasureVersionBuilder.PEASS_FOLDER_NAME);
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
      File rcaLogFile = folders.getRCALogFile(RTSLogFileTestUtil.COMMIT, RTSLogFileTestUtil.COMMIT_OLD);
      FileUtils.write(rcaLogFile, "This is a rca log test", StandardCharsets.UTF_8);
   }

   @Test
   public void testReading() throws JsonParseException, JsonMappingException, IOException {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, RTSLogFileTestUtil.COMMIT, RTSLogFileTestUtil.COMMIT_OLD);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, peassDemoConfig, null, null, 5, false, false, false, null);
      
      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getPeassRCAFolders()).thenReturn(new CauseSearchFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, "demo-vis2"));
      LogFileReader reader = new LogFileReader(visualizationFolders, peassConfig);

      TestMethodCall test = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod2");
      Map<TestCase, List<RCALevel>> rcaTestcases = reader.getRCATestcases();
      List<RCALevel> levels = rcaTestcases.get(test);
      Assert.assertEquals(1, levels.size());
      MatcherAssert.assertThat(levels.get(0).getLogFiles(), Matchers.hasSize(3));

      String rtsLog = reader.getRCALog();
      Assert.assertEquals("This is a rca log test", rtsLog);

   }
}
