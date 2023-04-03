package de.dagere.peass.ci.rts;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;

public class TestRTSLogSummary {

   @Test
   public void testEmptyResult() {
      RTSLogSummary summary = RTSLogSummary.createLogSummary(new HashMap<>(), new HashMap<>());

      Assert.assertFalse(summary.isErrorInCurrentCommitOccured());
      Assert.assertFalse(summary.isErrorInPredecessorCommitOccured());
      Assert.assertFalse(summary.isCommitContainsParametrizedwhithoutIndex());
   }

   @Test
   public void testFineResult() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false, false));
      rtsVmRunsPredecessor.put(new TestMethodCall("TestMe", "test"), new RTSLogData(null, null, null, success, false, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentCommitOccured());
      Assert.assertFalse(summary.isErrorInPredecessorCommitOccured());

      Assert.assertTrue(summary.isCommitContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertFalse(summary.isCommitContainsParametrizedwhithoutIndex());
   }

   @Test
   public void testParameterizedResultNoError() {
      Map<TestMethodCall, RTSLogData> rtsVmRuns = new HashMap<>();
      Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor = new HashMap<>();
      boolean success = true;
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentCommitOccured());
      Assert.assertFalse(summary.isErrorInPredecessorCommitOccured());

      Assert.assertTrue(summary.isCommitContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertTrue(summary.isCommitContainsParametrizedwhithoutIndex());
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
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test([0])"), new RTSLogData(null, null, null, success, true, false, false));
      rtsVmRuns.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false, false));
      success = false;
      rtsVmRunsPredecessor.put(TestMethodCall.createFromString("TestMe#test2([0])"), new RTSLogData(null, null, null, success, true, false, false));
      RTSLogSummary summary = RTSLogSummary.createLogSummary(rtsVmRuns, rtsVmRunsPredecessor);

      Assert.assertFalse(summary.isErrorInCurrentCommitOccured());
      Assert.assertTrue(summary.isErrorInPredecessorCommitOccured());

      Assert.assertTrue(summary.isCommitContainsSuccess());
      Assert.assertTrue(summary.isPredecessorContainsSuccess());

      Assert.assertTrue(summary.isCommitContainsParametrizedwhithoutIndex());
      Assert.assertTrue(summary.isPredecessorContainsParametrizedwhithoutIndex());

      AggregatedRTSResult aggregatedResult = new AggregatedRTSResult(summary, null);

      Assert.assertFalse(aggregatedResult.isRtsAllError());
      Assert.assertTrue(aggregatedResult.isRtsAnyError());
   }

}
