package de.dagere.peass.ci.logs;

import java.io.IOException;

import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.InvisibleAction;

public class LogAction extends InvisibleAction {
   
   private final String displayName;
   private final TestCase test;
   private final int vmId;
   private final String version;
   private final String logData;

   public LogAction(final TestCase test, final int vmId, final String version, final String logData) {
      this.displayName = "measurelog_" + test.toString().replace("#", "_") + "_" + vmId + "_" + version.substring(0, 6);
      this.test = test;
      this.vmId = vmId;
      this.version = version;
      this.logData = logData;
   }

   @Override
   public String getUrlName() {
      return displayName.replace("#", "_");
   }

   public TestCase getTest() {
      return test;
   }

   public int getVmId() {
      return vmId;
   }

   public String getVersion() {
      return version;
   }

   public String getLogData() {
      return logData;
   }

   public String getLog() throws IOException {
      return logData;
   }
}
