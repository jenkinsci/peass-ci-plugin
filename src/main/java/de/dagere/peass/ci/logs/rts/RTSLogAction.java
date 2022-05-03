package de.dagere.peass.ci.logs.rts;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RTSLogAction extends VisibleAction {
   private final String version;
   private final TestCase testcase;
   private final String cleanLog;
   private final String log;

   public RTSLogAction(int id, final String version, final TestCase testcase, final String cleanLog, final String log) {
      super(id);
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

   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return null;
   }

}
