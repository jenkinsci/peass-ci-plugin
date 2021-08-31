package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.InvisibleAction;

public class RTSLogAction extends InvisibleAction {
   private final String version;
   private final TestCase testcase;
   private final String cleanLog;
   private final String log;

   public RTSLogAction(final String version, final TestCase testcase, final String cleanLog, final String log) {
      this.version = version;
      this.testcase = testcase;
      this.cleanLog = cleanLog;
      this.log = log;
   }
   
   @Override
   public String getUrlName() {
      return "rtsLog_" + testcase.getLinkUsable() + "_" + version;
   }

   public String getVersion() {
      return version;
   }

   public TestCase getTestcase() {
      return testcase;
   }

   public String getCleanLog() {
      return cleanLog;
   }

   public String getLog() {
      return log;
   }

}