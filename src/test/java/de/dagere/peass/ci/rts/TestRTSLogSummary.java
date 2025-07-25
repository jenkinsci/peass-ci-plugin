package de.dagere.peass.ci.rts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;

class TestRTSLogSummary {

   @Test
   void testEmptyResult() {
      RTSLogSummary summary = RTSLogSummary.createLogSummary(new HashMap<>(), new HashMap<>());

      assertFalse(summary.isErrorInCurrentCommitOccured());
      assertFalse(summary.isErrorInPredecessorCommitOccured());
      assertFalse(summary.isCommitContainsParametrizedwhithoutIndex());
   }

   @Test
   void testFineResult() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false, false));
      rtsVmRunsPredecessor.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      assertFalse(summary.isErrorInCurrentCommitOccured());
      assertFalse(summary.isErrorInPredecessorCommitOccured());

      assertTrue(summary.isCommitContainsSuccess());
      assertTrue(summary.isPredecessorContainsSuccess());

      assertFalse(summary.isCommitContainsParametrizedwhithoutIndex());
   }

   @Test
   void testParameterizedResultNoError() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      assertFalse(summary.isErrorInCurrentCommitOccured());
      assertFalse(summary.isErrorInPredecessorCommitOccured());

      assertTrue(summary.isCommitContainsSuccess());
      assertTrue(summary.isPredecessorContainsSuccess());

      assertTrue(summary.isCommitContainsParametrizedwhithoutIndex());
      assertTrue(summary.isPredecessorContainsParametrizedwhithoutIndex());

      AggregatedRTSResult aggregatedResult = new AggregatedRTSResult(summary, null);

      assertFalse(aggregatedResult.isRtsAllError());
      assertFalse(aggregatedResult.isRtsAnyError());
   }

   @Test
   void testParameterizedResultError() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false, false));
      success = false;
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      assertFalse(summary.isErrorInCurrentCommitOccured());
      assertTrue(summary.isErrorInPredecessorCommitOccured());

      assertTrue(summary.isCommitContainsSuccess());
      assertTrue(summary.isPredecessorContainsSuccess());

      assertTrue(summary.isCommitContainsParametrizedwhithoutIndex());
      assertTrue(summary.isPredecessorContainsParametrizedwhithoutIndex());

      AggregatedRTSResult aggregatedResult = new AggregatedRTSResult(summary, null);

      assertFalse(aggregatedResult.isRtsAllError());
      assertTrue(aggregatedResult.isRtsAnyError());
   }
}
