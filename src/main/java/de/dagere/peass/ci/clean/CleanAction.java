package de.dagere.peass.ci.clean;

import hudson.model.Action;
import hudson.model.Job;

public class CleanAction implements Action {

   private Job<?, ?> project;

   public CleanAction(final Job<?, ?> project) {
      this.project = project;
   }
   
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
