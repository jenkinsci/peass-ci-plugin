package de.dagere.peass.ci.rts;

import de.dagere.peass.ci.VisibleAction;

public class RTSTraceAction extends VisibleAction {

   private final String testName;
   private final String trace;
   
   public RTSTraceAction(int id, final String displayName, final String trace) {
      super(id);
      this.testName = displayName;
      this.trace = trace;
      System.out.println("Added: " + displayName);
   }
   
   public String getTestName() {
      return testName;
   }
   
   public String getTrace() {
      return trace;
   }

   @Override
   public String getUrlName() {
      return "rts_" + testName.replace("#", "_");
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
