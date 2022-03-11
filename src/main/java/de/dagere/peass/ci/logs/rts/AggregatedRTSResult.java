package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.RTSResult;

public class AggregatedRTSResult {
   private final RTSLogSummary logSummary;
   private final RTSResult result;
   private final boolean rtsAnyError;
   /**
    * At least one test was run and no run was successful.
    */
   private final boolean rtsAllError;

   public AggregatedRTSResult(final RTSLogSummary logSummary, final RTSResult result) {
      this.logSummary = logSummary;
      this.result = result;
      if (logSummary != null) {

         rtsAnyError = logSummary.isErrorInCurrentVersionOccured() || logSummary.isErrorInPredecessorVersionOccured() && !(logSummary.isPredecessorContainsParametrizedwhithoutIndex() || logSummary.isVersionContainsParametrizedwhithoutIndex());
         rtsAllError = logSummary.isErrorInCurrentVersionOccured() && !logSummary.isVersionContainsSuccess() || !logSummary.isPredecessorContainsSuccess() && !(logSummary.isPredecessorContainsParametrizedwhithoutIndex() || logSummary.isVersionContainsParametrizedwhithoutIndex());

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
