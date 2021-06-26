package de.dagere.peass.ci.rts;

import hudson.model.InvisibleAction;

public class RTSTraceAction extends InvisibleAction {

   private final String testName;
   private final String trace;

   public RTSTraceAction(final String displayName, final String trace) {
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

}
