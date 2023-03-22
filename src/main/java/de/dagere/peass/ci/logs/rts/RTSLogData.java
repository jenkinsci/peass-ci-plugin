package de.dagere.peass.ci.logs.rts;

import java.io.File;

import de.dagere.peass.dependency.traces.TraceWriter;

public class RTSLogData {
   private final String commit;
   private final File methodFile;
   private final File cleanFile;
   private final boolean success;
   private final boolean isParameterizedWithoutIndex;
   private final boolean ignored;
   private final boolean removed;

   public RTSLogData(final String commit, final File methodFile, final File cleanFile, final boolean success, boolean isParameterizedWithoutIndex, final boolean ignored, boolean removed) {
      this.commit = commit;
      this.methodFile = methodFile;
      this.cleanFile = cleanFile;
      this.success = success;
      this.isParameterizedWithoutIndex = isParameterizedWithoutIndex;
      this.ignored = ignored;
      this.removed = removed;
   }

   public String getCommit() {
      return commit;
   }

   public String getShortCommit() {
      return TraceWriter.getShortCommit(commit);
   }

   public File getMethodFile() {
      return methodFile;
   }

   public File getCleanFile() {
      return cleanFile;
   }

   public boolean isSuccess() {
      return success;
   }

   public boolean isParameterizedWithoutIndex() {
      return isParameterizedWithoutIndex;
   }

   public boolean isIgnored() {
      return ignored;
   }
   
   public boolean isRemoved() {
      return removed;
   }
}
