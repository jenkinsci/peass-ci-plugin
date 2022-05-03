package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.VisibleAction;

public class ProcessSuccessLogAction extends VisibleAction {
   private final String displayName;
   private final String log;
   private final String version;

   public ProcessSuccessLogAction(int id, final String displayName, final String log, final String version) {
      super(id);
      this.displayName = displayName;
      this.log = log;
      this.version = version;
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

   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return null;
   }

}
