package de.peass.ci.rts;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class TestRTSLogSummary {

   @Test
   public void testEmptyResult() {
      RTSLogSummary summary = RTSLogSummary.createLogSummary(new HashMap<>(), new HashMap<>());

      Assert.assertFalse(summary.isErrorInCurrentVersionOccured());
      Assert.assertFalse(summary.isErrorInPredecessorVersionOccured());
      Assert.assertFalse(summary.isVersionContainsParametrizedwhithoutIndex());
   }

   @Test
   public void testFineResult() {
      Map<TestCase, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false));
      rtsVmRunsPredecessor.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentVersionOccured());
      Assert.assertFalse(summary.isErrorInPredecessorVersionOccured());

      Assert.assertTrue(summary.isVersionContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertFalse(summary.isVersionContainsParametrizedwhithoutIndex());
   }

   @Test
   public void testParameterizedResultNoError() {
      Map<TestCase, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestCase("TestMe#test([0])"), new RTSLogData(null, null, null, success, true));
      rtsVmRunsPredecessor.put(new TestCase("TestMe#test([0])"), new RTSLogData(null, null, null, success, true));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentVersionOccured());
      Assert.assertFalse(summary.isErrorInPredecessorVersionOccured());

      Assert.assertTrue(summary.isVersionContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertTrue(summary.isVersionContainsParametrizedwhithoutIndex());
      Assert.assertTrue(summary.isPredecessorContainsParametrizedwhithoutIndex());

      AggregatedRTSResult aggregatedResult = new AggregatedRTSResult(summary, null);

      Assert.assertFalse(aggregatedResult.isRtsAllError());
      Assert.assertFalse(aggregatedResult.isRtsAnyError());
   }

   @Test
   public void testParameterizedResultError() {
      Map<TestCase, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestCase, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestCase("TestMe#test([0])"), new RTSLogData(null, null, null, success, true));
      rtsVmRunsPredecessor.put(new TestCase("TestMe#test([0])"), new RTSLogData(null, null, null, success, true));
      rtsVmRuns.put(new TestCase("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true));
      success = false;
      rtsVmRunsPredecessor.put(new TestCase("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentVersionOccured());
      Assert.assertTrue(summary.isErrorInPredecessorVersionOccured());

      Assert.assertTrue(summary.isVersionContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertTrue(summary.isVersionContainsParametrizedwhithoutIndex());
      Assert.assertTrue(summary.isPredecessorContainsParametrizedwhithoutIndex());

      AggregatedRTSResult aggregatedResult = new AggregatedRTSResult(summary, null);

      Assert.assertFalse(aggregatedResult.isRtsAllError());
      Assert.assertTrue(aggregatedResult.isRtsAnyError());
   }

}
