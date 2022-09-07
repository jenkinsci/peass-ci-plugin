package de.dagere.peass.ci.logs.rca;

import de.dagere.peass.ci.logs.measurement.LogAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RCALogAction extends LogAction {

   private int level;

   public RCALogAction(int id, final TestCase test, final int vmId, final int level, final String commit, final String logData) {
      super(id, "rcalog_" + test.toString().replace("#", "_") + "_" + vmId + "_" + commit.substring(0, 6) + "_" + level, test, vmId, commit, logData);
      this.level = level;
   }
   
   public int getLevel() {
      return level;
   }
}
