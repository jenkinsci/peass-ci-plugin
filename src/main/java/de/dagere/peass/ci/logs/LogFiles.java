package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.Date;

public class LogFiles {

   /**
    * Attention! This class is used by the frontend, but file data are not accessible after a restart of Jenkins
    */
   private final File predecessor;
   private final File current;

   private final Date endDatePredecessor;
   private final Date endDateCurrent;

   private final boolean predecessorSuccess;
   private final boolean currentSuccess;

   public LogFiles(final File predecessor, final File current, final boolean predecessorSuccess, final boolean currentSuccess) {
      this.predecessor = predecessor;
      this.current = current;
      endDatePredecessor = new Date(predecessor.lastModified());
      endDateCurrent = new Date(current.lastModified());
      this.predecessorSuccess = predecessorSuccess;
      this.currentSuccess = currentSuccess;
   }

   public File getPredecessor() {
      return predecessor;
   }

   public File getCurrent() {
      return current;
   }

   public Date getEndDateCurrent() {
      // Just to meet spotbugs requirements - does not make sense
      return (Date) endDateCurrent.clone();
   }

   public Date getEndDatePredecessor() {
      // Just to meet spotbugs requirements - does not make sense
      return (Date) endDatePredecessor.clone();
   }

   public boolean isPredecessorSuccess() {
      return predecessorSuccess;
   }

   public boolean isCurrentSuccess() {
      return currentSuccess;
   }
}
