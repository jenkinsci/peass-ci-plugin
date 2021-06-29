package de.dagere.peass.ci.clean;

import hudson.model.Action;

public class CleanAction implements Action {

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/clean.png";
   }

   @Override
   public String getDisplayName() {
      return "Clean Peass-CI Cache";
   }

   @Override
   public String getUrlName() {
      return "cleanPeassCI";
   }

}
