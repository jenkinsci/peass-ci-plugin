package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.TestConstants;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class TestRTSLogFileReaderParameterized {

   private static final String COMMIT = "a12a0b7f4c162794fca0e7e3fcc6ea3b3a2cbc2b";
   private static final String COMMIT_OLD = "49f75e8877c2e9b7cf6b56087121a35fdd73ff8b";

   static final TestMethodCall TEST1 = new TestMethodCall("de.dagere.peass.ExampleTest", "test");

   private final File currentDir = new File("target/parameterized-demo_fullPeass");

   @BeforeEach
   public void initData() throws IOException {
      if (currentDir.exists()) {
         FileUtils.deleteDirectory(currentDir);
      }

      File sourceFile = new File(TestConstants.RESOURCE_FOLDER, "demo-results-logs/demo-parameterized_fullPeass");
      FileUtils.copyDirectory(sourceFile, currentDir);
   }

   @Test
   public void testParameterizedReading() {
      VisualizationFolderManager visualizationFoldersMock = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFoldersMock.getResultsFolders()).thenReturn(new ResultsFolders(currentDir, "parameterized-demo"));
      Mockito.when(visualizationFoldersMock.getPeassFolders()).thenReturn(new PeassFolders(new File(currentDir, "parameterized-demo_peass")));
      MeasurementConfig measurementConfig = Mockito.mock(MeasurementConfig.class);
      FixedCommitConfig fixedCommitConfig = new FixedCommitConfig();
      fixedCommitConfig.setCommit(COMMIT);
      fixedCommitConfig.setCommitOld(COMMIT_OLD);
      Mockito.when(measurementConfig.getFixedCommitConfig()).thenReturn(fixedCommitConfig);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, measurementConfig, null, null, 0, false, false, false, null);
      
      RTSLogFileReader reader = new RTSLogFileReader(visualizationFoldersMock, peassConfig);

      Map<TestMethodCall, RTSLogData> rtsVmRuns = reader.getRtsVmRuns(COMMIT, new TestSet());

      RTSLogData data = rtsVmRuns.get(new TestMethodCall("de.dagere.peass.ExampleTest", "test", "", "JUNIT_PARAMETERIZED-1"));
      Assert.assertNotNull(data);
      Assert.assertFalse(data.isParameterizedWithoutIndex());
      Assert.assertTrue(data.isSuccess());

      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = reader.getRtsVmRuns(COMMIT_OLD, new TestSet());
      RTSLogData dataImplicitParameterized = rtsVmRunsPredecessor.get(new TestMethodCall("de.dagere.peass.ExampleTest", "test", ""));
      Assert.assertNotNull(dataImplicitParameterized);
      Assert.assertTrue(dataImplicitParameterized.isParameterizedWithoutIndex());
      Assert.assertTrue(dataImplicitParameterized.isSuccess());
   }
}
