package de.dagere.peass.ci.rca;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class CommitRCAURLs{
   private Map<TestMethodCall, String> executionURLs = new LinkedHashMap<>();

   public Map<TestMethodCall, String> getExecutionURLs() {
      return executionURLs;
   }

   public void setExecutionURLs(Map<TestMethodCall, String> executionURLs) {
      this.executionURLs = executionURLs;
   }
}