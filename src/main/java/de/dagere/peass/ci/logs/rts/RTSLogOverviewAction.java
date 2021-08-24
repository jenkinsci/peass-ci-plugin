package de.dagere.peass.ci.logs.rts;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class RTSLogOverviewAction implements RunAction2 {
   private transient Run<?, ?> run;
   
   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      return "RTS Log Overview";
   }

   @Override
   public String getUrlName() {
      return "rtsLogOverview";
   }
   
   @Override
   public void onAttached(final Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(final Run<?, ?> run) {
      this.run = run;
   }
}
