package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.VisibleAction;

public class ProcessSuccessLogAction extends VisibleAction {
   private final String displayName;
   private final String log;
   private final String version;
   private final boolean success;

   public ProcessSuccessLogAction(final String displayName, final String log, final String version, final boolean success) {
      this.displayName = displayName;
      this.log = log;
      this.version = version;
      this.success = success;
   }
   
   @Override
   public String getUrlName() {
      return displayName.replace("#", "_");
   }

   public String getLog() {
      return log;
   }

   public String getVersion() {
      return version;
   }

   public boolean isSuccess() {
      return success;
   }
   
   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return null;
   }

}
