package de.dagere.peass.ci.logs.rts;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RTSLogOverviewAction extends VisibleAction {

   private Map<String, File> processSuccessRuns;
   private Map<String, Boolean> processSuccessRunSucceeded;
   private boolean staticChanges;
   private boolean staticallySelectedTests;
   private Map<TestCase, RTSLogData> vmRuns;
   private Map<TestCase, RTSLogData> predecessorVmRuns;
   private final String version, versionOld;
   private boolean redirectSubprocessOutputToFile;

   public RTSLogOverviewAction(final Map<String, File> processSuccessRuns, final Map<TestCase, RTSLogData> vmRuns, final Map<TestCase, RTSLogData> predecessorVmRuns,
         final Map<String, Boolean> processSuccessRunSucceeded, final String version, final String versionOld, final boolean redirectSubprocessOutputToFile) {
      this.processSuccessRuns = processSuccessRuns;
      this.vmRuns = vmRuns;
      this.predecessorVmRuns = predecessorVmRuns;
      this.processSuccessRunSucceeded = processSuccessRunSucceeded;
      this.version = version;
      this.versionOld = versionOld;
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
   }
   
   public Map<String, File> getProcessSuccessRuns() {
      return processSuccessRuns;
   }
   
   public Set<TestCase> getAllTests(){
      Set<TestCase> allTests = new TreeSet<>();
      allTests.addAll(vmRuns.keySet());
      allTests.addAll(predecessorVmRuns.keySet());
      return allTests;
   }

   public Map<TestCase, RTSLogData> getVmRuns() {
      return vmRuns;
   }

   public Map<TestCase, RTSLogData> getPredecessorVmRuns() {
      return predecessorVmRuns;
   }

   public Map<String, Boolean> getProcessSuccessRunSucceeded() {
      return processSuccessRunSucceeded;
   }

   public String getVersion() {
      return version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public boolean isStaticChanges() {
      return staticChanges;
   }

   public void setStaticChanges(final boolean staticChanges) {
      this.staticChanges = staticChanges;
   }

   public boolean isStaticallySelectedTests() {
      return staticallySelectedTests;
   }

   public void setStaticallySelectedTests(final boolean staticallySelectedTests) {
      this.staticallySelectedTests = staticallySelectedTests;
   }

   public boolean isRedirectSubprocessOutputToFile(){
      return redirectSubprocessOutputToFile;
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
