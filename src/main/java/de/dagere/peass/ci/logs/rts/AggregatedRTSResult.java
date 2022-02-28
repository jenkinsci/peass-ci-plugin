package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.RTSResult;

public class AggregatedRTSResult {
   private final RTSLogSummary logSummary;
   private final RTSResult result;
   private final boolean rtsAnyError;
   private final boolean rtsAllError;

   public AggregatedRTSResult(final RTSLogSummary logSummary, final RTSResult result) {
      this.logSummary = logSummary;
      this.result = result;
      if (logSummary != null) {
         rtsAnyError = logSummary.isErrorInCurrentVersionOccured() || logSummary.isErrorInPredecessorVersionOccured();
         rtsAllError = !logSummary.isVersionContainsSuccess() || !logSummary.isPredecessorContainsSuccess();
      } else {
         rtsAnyError = true;
         rtsAllError = true;
      }
   }

   public RTSLogSummary getLogSummary() {
      return logSummary;
   }

   public RTSResult getResult() {
      return result;
   }

   public boolean isRtsAnyError() {
      return rtsAnyError;
   }

   public boolean isRtsAllError() {
      return rtsAllError;
   }
}
