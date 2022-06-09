package de.dagere.peass.ci.remote;

import java.io.Serializable;
import java.util.List;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class RCAResult implements Serializable {
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
