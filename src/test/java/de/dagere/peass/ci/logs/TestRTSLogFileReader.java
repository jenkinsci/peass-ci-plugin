package de.dagere.peass.ci.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.TestConstants;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.data.TestSet;

class TestRTSLogFileReader {

   static final TestMethodCall TEST1 = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod1");
   static final TestMethodCall TEST2 = new TestMethodCall("de.test.CalleeTest", "onlyCallMethod2");

   private final RTSLogFileTestUtil util = new RTSLogFileTestUtil(TEST1, "demo-vis2");

   @BeforeEach
   void setUp() throws Exception {
      File source = new File(TestConstants.RESOURCE_FOLDER, "demo-results-logs/demo-vis2_peass");
      util.init(source);
   }

   @Test
   void testReading() {
      RTSLogFileReader reader = util.initializeReader();
      Map<String, File> testcases = reader.findProcessSuccessRuns();

      assertEquals(1, testcases.size());
      File testRunningFile = testcases.get("a23e385264c31def8dcda86c3cf64faa698c62d8");
      assertTrue(testRunningFile.exists());

      assertTrue(reader.isLogsExisting());

      Map<TestMethodCall, RTSLogData> rtsVmRuns = reader.getRtsVmRuns("a23e385264c31def8dcda86c3cf64faa698c62d8", new TestSet(), new TestSet());
      assertEquals(2, rtsVmRuns.size());

      checkFirstTest(rtsVmRuns);

      checkSecondTest(reader);

      String rtsLog = reader.getRTSLog();
      assertEquals("This is a rts log test", rtsLog);
   }

   private void checkSecondTest(RTSLogFileReader reader) {
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = reader.getRtsVmRuns("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f", new TestSet(), new TestSet());
      assertEquals(2, rtsVmRunsPredecessor.size());
      RTSLogData rtsLogData = rtsVmRunsPredecessor.get(TEST1);
      assertEquals("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f", rtsLogData.getCommit());
      assertTrue(rtsLogData.isSuccess());
      assertFalse(rtsLogData.isParameterizedWithoutIndex());
   }

   private void checkFirstTest(Map<TestMethodCall, RTSLogData> rtsVmRuns) {
      File dataFile1 = rtsVmRuns.get(TEST1).getMethodFile();
      assertTrue(dataFile1.exists());
      RTSLogData logDataTest2 = rtsVmRuns.get(TEST2);
      File dataFile2 = logDataTest2.getMethodFile();
      assertTrue(dataFile2.exists());
      assertFalse(logDataTest2.isSuccess());
      assertFalse(logDataTest2.isParameterizedWithoutIndex());
   }

   @Test
   void testReadingOnlyOverviewExists() throws Exception {
      FileUtils.deleteDirectory(RTSLogFileTestUtil.testFolder);

      RTSLogFileReader reader = util.initializeReader();
      assertTrue(reader.isLogsExisting());
   }
}
