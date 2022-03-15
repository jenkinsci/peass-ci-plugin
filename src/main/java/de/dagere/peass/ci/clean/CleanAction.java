package de.dagere.peass.ci.clean;

import de.dagere.peass.ci.Messages;
import hudson.model.Action;

public class CleanAction implements Action {

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/clean.png";
   }

   @Override
   public String getDisplayName() {
      return Messages.CleanAction_DisplayName();
   }

   @Override
   public String getUrlName() {
      return "cleanPeassCI";
   }

}
