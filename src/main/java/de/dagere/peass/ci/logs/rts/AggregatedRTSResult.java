package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.RTSResult;

public class AggregatedRTSResult {
   private final RTSLogSummary logSummary;
   private final RTSResult result;
   private boolean rtsError = false;

   public AggregatedRTSResult(final RTSLogSummary logSummary, final RTSResult result) {
      this.logSummary = logSummary;
      this.result = result;
   }

   public RTSLogSummary getLogSummary() {
      return logSummary;
   }

   public RTSResult getResult() {
      return result;
   }

   public boolean isRtsError() {
      return rtsError;
   }

   public void setRtsError(final boolean rtsError) {
      this.rtsError = rtsError;
   }

}
