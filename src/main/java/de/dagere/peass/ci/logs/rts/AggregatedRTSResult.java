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
      boolean containsParametrizedwhithoutIndex =false;
      boolean isContainsSuccess=false;
      if (logSummary != null) {
         containsParametrizedwhithoutIndex= logSummary.isPredecessorContainsParametrizedwhithoutIndex() || logSummary.isVersionContainsParametrizedwhithoutIndex();
         isContainsSuccess = logSummary.isVersionContainsSuccess() || logSummary.isPredecessorContainsSuccess();

         rtsAnyError = logSummary.isErrorInCurrentVersionOccured() || logSummary.isErrorInPredecessorVersionOccured() && !containsParametrizedwhithoutIndex;
         rtsAllError = logSummary.isErrorInCurrentVersionOccured() && !isContainsSuccess && !containsParametrizedwhithoutIndex;

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
