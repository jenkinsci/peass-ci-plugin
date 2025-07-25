package de.dagere.peass.ci.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.TestConstants;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.data.TestSet;

class TestRTSLogFileReaderMultimodule {

   private static final TestMethodCall TEST1 = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod1", "moduleA");

   private final RTSLogFileTestUtil util = new RTSLogFileTestUtil(TEST1, "demo-vis2-multimodule");

   @BeforeEach
   void setUp() throws Exception {
      File source = new File(TestConstants.RESOURCE_FOLDER, "demo-results-logs/demo-vis2-multimodule_peass");
      util.init(source);
   }

   @Test
   void testReading() {
      RTSLogFileReader reader = util.initializeReader();
      Map<String, File> testcases = reader.findProcessSuccessRuns();

      assertEquals(1, testcases.size());
      File testRunningFile = testcases.get(RTSLogFileTestUtil.COMMIT);
      assertTrue(testRunningFile.exists());

      assertTrue(reader.isLogsExisting());

      Map<TestMethodCall, RTSLogData> rtsVmRuns = reader.getRtsVmRuns(RTSLogFileTestUtil.COMMIT, new TestSet(), new TestSet());
      assertEquals(2, rtsVmRuns.size());

      File dataFile1 = rtsVmRuns.get(TestRTSLogFileReader.TEST1).getMethodFile();
      assertTrue(dataFile1.exists());
      RTSLogData logDataTest2 = rtsVmRuns.get(TestRTSLogFileReader.TEST2);
      File dataFile2 = logDataTest2.getMethodFile();
      assertTrue(dataFile2.exists());
      assertFalse(logDataTest2.isSuccess());

      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = reader.getRtsVmRuns(RTSLogFileTestUtil.COMMIT_OLD, new TestSet(), new TestSet());
      assertEquals(2, rtsVmRunsPredecessor.size());
      RTSLogData rtsLogData = rtsVmRunsPredecessor.get(TestRTSLogFileReader.TEST1);
      assertEquals(RTSLogFileTestUtil.COMMIT_OLD, rtsLogData.getCommit());
      assertTrue(rtsLogData.isSuccess());

      String rtsLog = reader.getRTSLog();
      assertEquals("This is a rts log test", rtsLog);
   }

}
