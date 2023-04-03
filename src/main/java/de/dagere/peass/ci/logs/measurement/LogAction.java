package de.dagere.peass.ci.logs.measurement;

import java.io.IOException;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.peass.ci.VisibleAction;

public class LogAction extends VisibleAction {
   
   protected final String displayName;
   private final TestCase test;
   private final int vmId;
   private final String commit;
   private final String logData;

   public LogAction(int id, final TestCase test, final int vmId, final String commit, final String logData) {
      this(id, "measurelog_" + test.toString().replace("#", "_") + "_" + vmId + "_" + commit.substring(0, 6), test, vmId, commit, logData);
   }

   protected LogAction(int id, final String displayName, final TestCase test, final int vmId, final String commit, final String logData) {
      super(id);
      this.displayName = displayName;
      this.test = test;
      this.vmId = vmId;
      this.commit = commit;
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
      return commit;
   }

   public String getLogData() {
      return logData;
   }

   public String getLog() throws IOException {
      return logData;
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
