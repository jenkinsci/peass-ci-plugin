package de.dagere.peass.ci.rca;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.utils.Constants;

class CommitRCAURLs{
   private Map<TestMethodCall, String> executionURLs = new LinkedHashMap<>();

   public Map<TestMethodCall, String> getExecutionURLs() {
      return executionURLs;
   }

   public void setExecutionURLs(Map<TestMethodCall, String> executionURLs) {
      this.executionURLs = executionURLs;
   }
}

public class RCAMapping {
   private Map<String, CommitRCAURLs> commits = new LinkedHashMap<>();

   public Map<String, CommitRCAURLs> getCommits() {
      return commits;
   }

   public void setCommits(Map<String, CommitRCAURLs> commits) {
      this.commits = commits;
   }
   
   public static void main(String[] args) throws JsonProcessingException {
      CommitRCAURLs value = new CommitRCAURLs();
      value.getExecutionURLs().put(new TestMethodCall("MyClass", "myMethod"), "3");
      
      RCAMapping rcaMapping = new RCAMapping();
      rcaMapping.getCommits().put("commit1", value);
      
      System.out.println(Constants.OBJECTMAPPER.writeValueAsString(rcaMapping));
      
   }

   public void addMapping(String commit, TestMethodCall testMethodCall, String url) {
      CommitRCAURLs urls = commits.get(commit);
      if (urls == null) {
         urls = new CommitRCAURLs();
         commits.put(commit, urls);
      }
      urls.getExecutionURLs().put(testMethodCall, url);
   }
   
}
