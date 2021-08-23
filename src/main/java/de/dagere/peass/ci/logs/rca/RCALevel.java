package de.dagere.peass.ci.logs.rca;

import java.util.List;

import de.dagere.peass.ci.logs.LogFiles;

public class RCALevel {
   private final List<LogFiles> logFiles;

   public RCALevel(final List<LogFiles> logFiles) {
      this.logFiles = logFiles;
   }

   public List<LogFiles> getLogFiles() {
      return logFiles;
   }
}
