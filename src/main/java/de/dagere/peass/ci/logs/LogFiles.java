package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.Date;

public class LogFiles {
   private final File predecessor;
   private final File current;

   private final Date endDatePredecessor;
   private final Date endDateCurrent;

   public LogFiles(final File predecessor, final File current) {
      this.predecessor = predecessor;
      this.current = current;
      endDatePredecessor = new Date(predecessor.lastModified());
      endDateCurrent = new Date(current.lastModified());
   }

   public File getPredecessor() {
      return predecessor;
   }

   public File getCurrent() {
      return current;
   }

   public Date getEndDateCurrent() {
      return endDateCurrent;
   }

   public Date getEndDatePredecessor() {
      return endDatePredecessor;
   }
}
