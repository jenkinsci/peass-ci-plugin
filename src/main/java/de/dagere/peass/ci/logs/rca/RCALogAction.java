package de.dagere.peass.ci.logs.rca;

import de.dagere.peass.ci.logs.measurement.LogAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RCALogAction extends LogAction {

   private int level;

   public RCALogAction(final TestCase test, final int vmId, final int level, final String version, final String logData) {
      super("rcalog_" + test.toString().replace("#", "_") + "_" + vmId + "_" + version.substring(0, 6) + "_" + level, test, vmId, version, logData);
      this.level = level;
   }
   
   public int getLevel() {
      return level;
   }
}
