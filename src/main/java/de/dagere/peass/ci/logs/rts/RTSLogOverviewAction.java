package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.util.Map;

import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class RTSLogOverviewAction implements RunAction2 {
   
   private transient Run<?, ?> run;
   private Map<String, File> processSuccessRuns;
   private Map<TestCase, LogFiles> vmRuns;
   
   public RTSLogOverviewAction(final Map<String, File> processSuccessRuns, final Map<TestCase, LogFiles> vmRuns) {
      this.processSuccessRuns = processSuccessRuns;
      this.vmRuns = vmRuns;
   }

   public Map<String, File> getProcessSuccessRuns() {
      return processSuccessRuns;
   }
   
   public Map<TestCase, LogFiles> getVmRuns() {
      return vmRuns;
   }
   
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
