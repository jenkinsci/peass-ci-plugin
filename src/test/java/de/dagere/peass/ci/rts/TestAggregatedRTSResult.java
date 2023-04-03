package de.dagere.peass.ci.rts;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.RTSResult;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;

public class TestAggregatedRTSResult {

   @Test
   public void testNoError() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(false);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(true);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(true);

      RTSResult rtsResult = new RTSResult(new HashSet<TestMethodCall>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);
      
      Assert.assertFalse(result.isRtsAllError());
      Assert.assertFalse(result.isRtsAnyError());
   }

   @Test
   public void testNoTest() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(false);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(false);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(false);

      RTSResult rtsResult = new RTSResult(new HashSet<TestMethodCall>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);
      
      Assert.assertFalse(result.isRtsAllError());
      Assert.assertFalse(result.isRtsAnyError());
   }

   @Test
   public void testAnError() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(true);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(true);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(true);

      RTSResult rtsResult = new RTSResult(new HashSet<TestMethodCall>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);
      
      Assert.assertFalse(result.isRtsAllError());
      Assert.assertTrue(result.isRtsAnyError());
   }
}
