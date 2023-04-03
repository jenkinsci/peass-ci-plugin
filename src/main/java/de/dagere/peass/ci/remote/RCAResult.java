package de.dagere.peass.ci.remote;

import java.io.Serializable;
import java.util.List;

import de.dagere.nodeDiffDetector.data.TestCase;


public class RCAResult implements Serializable {
   private static final long serialVersionUID = -1651195171472844693L;
   
   private final boolean success;
   private final List<TestCase> failedTests;

   public RCAResult(boolean success, List<TestCase> failedTests) {
      this.success = success;
      this.failedTests = failedTests;
   }

   public boolean isSuccess() {
      return success;
   }

   public List<TestCase> getFailedTests() {
      return failedTests;
   }

}
