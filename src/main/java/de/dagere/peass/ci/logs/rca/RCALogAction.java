package de.dagere.peass.ci.logs.rca;

import de.dagere.peass.ci.logs.LogAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RCALogAction extends LogAction{
   
   private int level;

   public RCALogAction(final TestCase test, final int vmId, final int level, final String version, final String logData) {
      super(test, vmId, version, logData);
      this.level = level;
   }

   @Override
   public String getUrlName() {
      return displayName.replace("#", "_") + "_" + level;
   }
}
