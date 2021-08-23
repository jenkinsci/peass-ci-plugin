package de.dagere.peass.ci.logs.rca;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class RCALogOverviewAction implements RunAction2 {
   
   private transient Run<?, ?> run;
   private String version;
   private String versionOld;

   public RCALogOverviewAction(final String version, final String versionOld) {
      this.version = version;
      this.versionOld = versionOld;
   }

   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      // TODO Auto-generated method stub
      return "Root Cause Analysis Log Overview";
   }

   @Override
   public String getUrlName() {
      return "rcaLogOverview";
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
