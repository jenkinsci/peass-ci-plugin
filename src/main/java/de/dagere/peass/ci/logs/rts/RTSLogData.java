package de.dagere.peass.ci.logs.rts;

import java.io.File;

import de.dagere.peass.dependency.traces.TraceWriter;

public class RTSLogData {
   private final String version;
   private final File methodFile;
   private final File cleanFile;
   private final boolean success;

   public RTSLogData(final String version, final File methodFile, final File cleanFile, final boolean success) {
      this.version = version;
      this.methodFile = methodFile;
      this.cleanFile = cleanFile;
      this.success = success;
   }

   public String getVersion() {
      return version;
   }

   public String getShortVersion() {
      return TraceWriter.getShortVersion(version);
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
}
