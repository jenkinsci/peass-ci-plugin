package de.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.logs.RTSLogFileReader;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class TestRTSLogFileReaderMultimodule {

   private static final TestMethodCall TEST1 = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod1", "moduleA");

   private RTSLogFileTestUtil util = new RTSLogFileTestUtil(TEST1, "demo-vis2-multimodule");

   @BeforeEach
   public void init() throws IOException {
      File source = new File("src/test/resources/demo-results-logs/demo-vis2-multimodule_peass");
      util.init(source);
   }

   @Test
   public void testReading() throws JsonParseException, JsonMappingException, IOException {
      RTSLogFileReader reader = util.initializeReader();
      Map<String, File> testcases = reader.findProcessSuccessRuns();

      Assert.assertEquals(1, testcases.size());
      File testRunningFile = testcases.get(RTSLogFileTestUtil.COMMIT);
      Assert.assertTrue(testRunningFile.exists());

      Assert.assertTrue(reader.isLogsExisting());

      Map<TestMethodCall, RTSLogData> rtsVmRuns = reader.getRtsVmRuns(RTSLogFileTestUtil.COMMIT);
      Assert.assertEquals(2, rtsVmRuns.size());

      File dataFile1 = rtsVmRuns.get(TestRTSLogFileReader.TEST1).getMethodFile();
      Assert.assertTrue(dataFile1.exists());
      RTSLogData logDataTest2 = rtsVmRuns.get(TestRTSLogFileReader.TEST2);
      File dataFile2 = logDataTest2.getMethodFile();
      Assert.assertTrue(dataFile2.exists());
      Assert.assertFalse(logDataTest2.isSuccess());

      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = reader.getRtsVmRuns(RTSLogFileTestUtil.COMMIT_OLD);
      Assert.assertEquals(2, rtsVmRunsPredecessor.size());
      RTSLogData rtsLogData = rtsVmRunsPredecessor.get(TestRTSLogFileReader.TEST1);
      Assert.assertEquals(RTSLogFileTestUtil.COMMIT_OLD, rtsLogData.getVersion());
      Assert.assertTrue(rtsLogData.isSuccess());

      String rtsLog = reader.getRTSLog();
      Assert.assertEquals("This is a rts log test", rtsLog);
   }

}
