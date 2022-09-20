package de.peass.ci.rts;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
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
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false));
      rtsVmRunsPredecessor.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentVersionOccured());
      Assert.assertFalse(summary.isErrorInPredecessorVersionOccured());

      Assert.assertTrue(summary.isVersionContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertFalse(summary.isVersionContainsParametrizedwhithoutIndex());
   }

   @Test
   public void testParameterizedResultNoError() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false));
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
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false));
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false));
      success = false;
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false));
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
