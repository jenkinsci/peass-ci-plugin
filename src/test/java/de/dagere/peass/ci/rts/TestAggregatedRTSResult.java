package de.dagere.peass.ci.rts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.ci.RTSResult;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;

class TestAggregatedRTSResult {

   @Test
   void testNoError() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(false);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(true);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(true);

      RTSResult rtsResult = new RTSResult(new HashSet<>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);

      assertFalse(result.isRtsAllError());
      assertFalse(result.isRtsAnyError());
   }

   @Test
   void testNoTest() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(false);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(false);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(false);

      RTSResult rtsResult = new RTSResult(new HashSet<>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);

      assertFalse(result.isRtsAllError());
      assertFalse(result.isRtsAnyError());
   }

   @Test
   void testAnError() {
      RTSLogSummary summary = Mockito.mock(RTSLogSummary.class);
      Mockito.when(summary.isErrorInCurrentCommitOccured()).thenReturn(true);
      Mockito.when(summary.isErrorInPredecessorCommitOccured()).thenReturn(false);
      Mockito.when(summary.isPredecessorContainsSuccess()).thenReturn(true);
      Mockito.when(summary.isCommitContainsSuccess()).thenReturn(true);

      RTSResult rtsResult = new RTSResult(new HashSet<>(), true);
      AggregatedRTSResult result = new AggregatedRTSResult(summary, rtsResult);

      assertFalse(result.isRtsAllError());
      assertTrue(result.isRtsAnyError());
   }
}
