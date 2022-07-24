package de.dagere.peass.ci.peassOverview;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.ci.peassOverview.classification.ClassifiedProject;
import de.dagere.peass.ci.peassOverview.classification.TestcaseClassification;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class ProjectOverviewStatistic {
   private int commitsWithSourceChange;
   private int testsWithSourceChange;
   private int commitsWithChange, testsWithChange;
   private int unmeasuredTests;
   private Map<String, Integer> categoryTestCount = new LinkedHashMap<>();
   private Map<String, Integer> categoryCommitCount = new LinkedHashMap<>();

   public ProjectOverviewStatistic() {

   }

   public int getCommitsWithSourceChange() {
      return commitsWithSourceChange;
   }

   public void setCommitsWithSourceChange(int commitsWithSourceChange) {
      this.commitsWithSourceChange = commitsWithSourceChange;
   }

   public int getTestsWithSourceChange() {
      return testsWithSourceChange;
   }

   public void setTestsWithSourceChange(int testsWithSourceChange) {
      this.testsWithSourceChange = testsWithSourceChange;
   }

   public int getCommitsWithChange() {
      return commitsWithChange;
   }

   public void setCommitsWithChange(int commitWithChange) {
      this.commitsWithChange = commitWithChange;
   }

   public int getTestsWithChange() {
      return testsWithChange;
   }

   public void setTestsWithChange(int testsWithChange) {
      this.testsWithChange = testsWithChange;
   }

   public int getUnmeasuredTests() {
      return unmeasuredTests;
   }

   public void setUnmeasuredTests(int unmeasuredTests) {
      this.unmeasuredTests = unmeasuredTests;
   }

   public Map<String, Integer> getCategoryTestCount() {
      return categoryTestCount;
   }

   public void setCategoryTestCount(Map<String, Integer> categoryTestCount) {
      this.categoryTestCount = categoryTestCount;
   }

   public Map<String, Integer> getCategoryCommitCount() {
      return categoryCommitCount;
   }

   public void setCategoryCommitCount(Map<String, Integer> categoryCommitCount) {
      this.categoryCommitCount = categoryCommitCount;
   }

   public void increaseCategoryTestCount(String category) {
      Integer count = categoryTestCount.get(category);
      if (count == null) {
         count = 1;
      } else {
         count = count + 1;
      }
      categoryTestCount.put(category, count);
   }

   public void increaseCategoryCommitCount(String category) {
      Integer count = categoryCommitCount.get(category);
      if (count == null) {
         count = 1;
      } else {
         count = count + 1;
      }
      categoryCommitCount.put(category, count);
   }

   public static ProjectOverviewStatistic getFromClassification(String projectName, ProjectData projectData, ClassifiedProject classificationData) {
      ProjectOverviewStatistic statistic = new ProjectOverviewStatistic();

      int commitCount = projectData.getStatistics().getStatistics().size();
      statistic.setCommitsWithSourceChange(commitCount);
      int tests = projectData.getStatistics().getTestCount();
      statistic.setTestsWithSourceChange(tests);
      statistic.setCommitsWithChange(projectData.getChanges().getCommitChanges().size());
      statistic.setTestsWithChange(projectData.getChanges().getChangeCount());
      // statistic.setCommitsWithChange();

      Set<String> commits = new LinkedHashSet<>();
      commits.addAll(classificationData.getChangeClassifications().keySet());
      commits.addAll(classificationData.getUnmeasuredClassifications().keySet());

      for (String commit : commits) {
         Set<String> classificationsInThisCommit = new HashSet<>();

         addChangeClassifications(classificationData, statistic, commit, classificationsInThisCommit);
         addUnmeasuredClassifications(classificationData, statistic, commit, classificationsInThisCommit);

         CommitStaticSelection commitSelection = projectData.getSelection().getCommits().get(commit);
         if (commitSelection != null) {
            int unmeasured = statistic.getUnmeasuredTests();
            Map<TestCase, TestcaseStatistic> commitStatistic = projectData.getStatistics().getStatistics().get(commit);
            for (TestCase test : commitSelection.getTests().getTests()) {
               if (commitStatistic == null) {
                  unmeasured++;
               } else if (commitStatistic.get(test) == null) {
                  unmeasured++;
               }
            }
            statistic.setUnmeasuredTests(unmeasured);
         }

         for (String commitClassification : classificationsInThisCommit) {
            statistic.increaseCategoryCommitCount(commitClassification);
         }
      }
      return statistic;
   }

   public static ProjectOverviewStatistic getSumStatistic(Collection<ProjectOverviewStatistic> statistics) {
      ProjectOverviewStatistic result = new ProjectOverviewStatistic();

      for (ProjectOverviewStatistic statistic : statistics) {
         result.setCommitsWithChange(result.getCommitsWithChange() + statistic.getCommitsWithChange());
         result.setCommitsWithSourceChange(result.getCommitsWithSourceChange() + statistic.getCommitsWithSourceChange());
         result.setTestsWithChange(result.getTestsWithChange() + statistic.getTestsWithChange());
         result.setTestsWithSourceChange(result.getTestsWithSourceChange() + statistic.getTestsWithSourceChange());
         result.setUnmeasuredTests(result.getUnmeasuredTests() + statistic.getUnmeasuredTests());

         for (String category : statistic.getCategoryCommitCount().keySet()) {
            Integer categoryCommitCount = result.getCategoryCommitCount().get(category);
            if (categoryCommitCount == null) {
               categoryCommitCount = 0;
            }
            categoryCommitCount += statistic.getCategoryCommitCount().get(category);
            result.getCategoryCommitCount().put(category, categoryCommitCount);

            Integer categoryTestCount = result.getCategoryTestCount().get(category);
            if (categoryTestCount == null) {
               categoryTestCount = 0;
            }
            categoryTestCount += statistic.getCategoryTestCount().get(category);
            result.getCategoryTestCount().put(category, categoryTestCount);
         }
      }

      return result;
   }

   private static void addUnmeasuredClassifications(ClassifiedProject classificationData, ProjectOverviewStatistic statistic, String commit,
         Set<String> classificationsInThisCommit) {
      TestcaseClassification unmeasuredClassification = classificationData.getUnmeasuredClassification(commit);
      if (unmeasuredClassification != null) {
         Map<String, String> unmeasuredClassificationMap = unmeasuredClassification.getClassifications();
         for (Entry<String, String> testcaseUnmeasuredClassification : unmeasuredClassificationMap.entrySet()) {
            String chosenClassification = testcaseUnmeasuredClassification.getValue();
            statistic.increaseCategoryTestCount(chosenClassification);
            classificationsInThisCommit.add(chosenClassification);
         }
      }
   }

   private static void addChangeClassifications(ClassifiedProject classificationData, ProjectOverviewStatistic statistic, String commit, Set<String> classificationsInThisCommit) {
      TestcaseClassification measuredClassification = classificationData.getChangeClassifications().get(commit);
      if (measuredClassification != null) {
         Map<String, String> measuredClassificationMap = measuredClassification.getClassifications();
         for (Entry<String, String> testcaseClassification : measuredClassificationMap.entrySet()) {
            String chosenClassification = testcaseClassification.getValue();
            statistic.increaseCategoryTestCount(chosenClassification);
            classificationsInThisCommit.add(chosenClassification);
         }
      }
   }

}