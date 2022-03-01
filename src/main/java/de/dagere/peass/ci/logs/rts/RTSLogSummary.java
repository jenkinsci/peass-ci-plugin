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
   private final boolean versionContainsSuccess;
   private final boolean predecessorContainsSuccess;

   public RTSLogSummary(final boolean errorInCurrentVersionOccured, final boolean errorInPredecessorVersionOccured, boolean versionContainsSuccess,
         boolean predecessorContainsSuccess) {
      this.errorInCurrentVersionOccured = errorInCurrentVersionOccured;
      this.errorInPredecessorVersionOccured = errorInPredecessorVersionOccured;
      this.versionContainsSuccess = versionContainsSuccess;
      this.predecessorContainsSuccess = predecessorContainsSuccess;
   }

   public boolean isErrorInCurrentVersionOccured() {
      return errorInCurrentVersionOccured;
   }

   public boolean isErrorInPredecessorVersionOccured() {
      return errorInPredecessorVersionOccured;
   }

   public boolean isVersionContainsSuccess() {
      return versionContainsSuccess;
   }

   public boolean isPredecessorContainsSuccess() {
      return predecessorContainsSuccess;
   }

}
