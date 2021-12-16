package de.dagere.peass.ci.logs.rts;

/**
 * This summarizes logs for showing information in the regular step action.
 * 
 * Currently, it only contains a boolean, but it is expected to contain more information for the end user soon.
 * 
 * @author reichelt
 *
 */
public class RTSLogSummary {
   
   private final boolean errorInCurrentVersionOccured;
   private final boolean errorInPredecessorVersionOccured;

   public RTSLogSummary(final boolean errorInCurrentVersionOccured, final boolean errorInPredecessorVersionOccured) {
      this.errorInCurrentVersionOccured = errorInCurrentVersionOccured;
      this.errorInPredecessorVersionOccured = errorInPredecessorVersionOccured;
   }

   public boolean isErrorInCurrentVersionOccured() {
      return errorInCurrentVersionOccured;
   }

   public boolean isErrorInPredecessorVersionOccured() {
      return errorInPredecessorVersionOccured;
   }

}
