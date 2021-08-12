package de.dagere.peass.ci.logs;

import java.io.File;

public class LogFiles {
   private final File predecessor;
   private final File current;

   public LogFiles(final File predecessor, final File current) {
      this.predecessor = predecessor;
      this.current = current;
   }

   public File getPredecessor() {
      return predecessor;
   }

   public File getCurrent() {
      return current;
   }

}
