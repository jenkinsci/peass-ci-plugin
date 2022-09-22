package de.dagere.peass.ci.rca;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class RCAMapping {
   private Map<String, CommitRCAURLs> commits = new LinkedHashMap<>();

   public Map<String, CommitRCAURLs> getCommits() {
      return commits;
   }

   public void setCommits(Map<String, CommitRCAURLs> commits) {
      this.commits = commits;
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
