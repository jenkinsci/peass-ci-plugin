package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.RTSResult;

public class AggregatedRTSResult {
   private final RTSLogSummary logSummary;
   private final RTSResult result;

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

}
