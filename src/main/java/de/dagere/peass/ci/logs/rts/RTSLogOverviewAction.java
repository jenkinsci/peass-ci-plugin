package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.util.Map;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RTSLogOverviewAction extends VisibleAction {
   
   private Map<String, File> processSuccessRuns;
   private Map<TestCase, RTSLogData> vmRuns;
   private Map<TestCase, RTSLogData> predecessorVmRuns;

   public RTSLogOverviewAction(final Map<String, File> processSuccessRuns, final Map<TestCase, RTSLogData> vmRuns, final Map<TestCase, RTSLogData> predecessorVmRuns) {
      this.processSuccessRuns = processSuccessRuns;
      this.vmRuns = vmRuns;
      this.predecessorVmRuns = predecessorVmRuns;
   }

   public Map<String, File> getProcessSuccessRuns() {
      return processSuccessRuns;
   }

   public Map<TestCase, RTSLogData> getVmRuns() {
      return vmRuns;
   }

   public Map<TestCase, RTSLogData> getPredecessorVmRuns() {
      return predecessorVmRuns;
   }

   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      return "Regression Test Selection Log Overview";
   }

   @Override
   public String getUrlName() {
      return "rtsLogOverview";
   }

   
}
