package de.dagere.peass.ci.logs.rts;

import java.io.File;

public class RTSLogData {
   private final String version;
   private final File methodFile;
   private final File cleanFile;

   public RTSLogData(final String version, final File methodFile, final File cleanFile) {
      this.version = version;
      this.methodFile = methodFile;
      this.cleanFile = cleanFile;
   }

   public String getVersion() {
      return version;
   }

   public File getMethodFile() {
      return methodFile;
   }

   public File getCleanFile() {
      return cleanFile;
   }

}
