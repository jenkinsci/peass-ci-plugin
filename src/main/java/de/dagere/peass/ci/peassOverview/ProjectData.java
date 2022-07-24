package de.dagere.peass.ci.peassOverview;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;

public class ProjectData {
   private final StaticTestSelection selection;
   private final ProjectChanges changes;
   private final ProjectStatistics statistics;
   private final boolean containsError;

   public ProjectData(StaticTestSelection selection, ProjectChanges changes, ProjectStatistics statistics, boolean containsError) {
      this.selection = selection;
      this.changes = changes;
      this.statistics = statistics;
      this.containsError = containsError;
   }

   public StaticTestSelection getSelection() {
      return selection;
   }

   public ProjectChanges getChanges() {
      return changes;
   }
   
   public ProjectStatistics getStatistics() {
      return statistics;
   }

   public boolean isContainsError() {
      return containsError;
   }

   public List<ChangeLine> getChangeLines() {
      List<ChangeLine> result = new LinkedList<>();
      if (selection != null) {

         for (Map.Entry<String, CommitStaticSelection> commitEntry : selection.getCommits().entrySet()) {
            String commit = commitEntry.getKey();
            CommitStaticSelection commitStaticSelection = commitEntry.getValue();
            addCommitData(result, commit, commitStaticSelection);
         }
      }

      return result;
   }

   private void addCommitData(List<ChangeLine> result, String commit, CommitStaticSelection commitStaticSelection) {
      Map<ChangedEntity, TestSet> changedClazzes = commitStaticSelection.getChangedClazzes();

      Map<TestCase, Set<String>> oneTestCausingChanges = new LinkedHashMap<>();
      List<String> changesWithNoTest = new LinkedList<>();
      fillOneOrLessTests(changedClazzes, oneTestCausingChanges, changesWithNoTest);

      addOneTestCausingChanges(result, commit, oneTestCausingChanges);

      addNoneTestCausingChanges(result, commit, changesWithNoTest);
   }

   private void fillOneOrLessTests(Map<ChangedEntity, TestSet> changedClazzes, Map<TestCase, Set<String>> oneTestCausingChanges, List<String> changesWithNoTest) {
      for (Map.Entry<ChangedEntity, TestSet> entry : changedClazzes.entrySet()) {

         String changedEntity = entry.getKey().toString();
         if (entry.getValue().getTests().size() > 0) {
            for (TestCase test : entry.getValue().getTests()) {
               if (!oneTestCausingChanges.containsKey(test)) {
                  oneTestCausingChanges.put(test, new TreeSet<>());
               }
               oneTestCausingChanges.get(test).add(changedEntity);
            }
         } else if (entry.getValue().getTests().size() == 1) {
            TestCase test = entry.getValue().getTests().iterator().next();
            if (!oneTestCausingChanges.containsKey(test)) {
               oneTestCausingChanges.put(test, new TreeSet<>());
            }
            oneTestCausingChanges.get(test).add(changedEntity);
         } else {
            changesWithNoTest.add(changedEntity);
         }
      }
   }

   private void addNoneTestCausingChanges(List<ChangeLine> result, String commit, List<String> changesWithNoTest) {
      if (changesWithNoTest.size() > 0) {
         ChangeLine line = new ChangeLine(commit, changesWithNoTest, "none", Double.NaN);
         result.add(line);
      }
   }

   private void addOneTestCausingChanges(List<ChangeLine> result, String commit, Map<TestCase, Set<String>> oneTestCausingChanges) {
      for (Map.Entry<TestCase, Set<String>> oneTestEntry : oneTestCausingChanges.entrySet()) {
         TestCase test = oneTestEntry.getKey();

         double changeValue = getChangeValue(commit, test);

         List<String> changesAsList = new LinkedList<>();
         changesAsList.addAll(oneTestEntry.getValue());
         ChangeLine line = new ChangeLine(commit, changesAsList, test.toString(), changeValue);
         result.add(line);
      }
   }

   private double getChangeValue(String commit, TestCase test) {
      TestcaseStatistic testcaseStatistic = getTestcaseStatistic(commit, test);

      Changes commitChanges = changes.getCommitChanges(commit);
      Change change = commitChanges.getChange(test);

      double changeValue = getPrintableChangeValue(testcaseStatistic, change);
      return changeValue;
   }

   private TestcaseStatistic getTestcaseStatistic(String commit, TestCase test) {
      Map<TestCase, TestcaseStatistic> commitStatistics = statistics.getStatistics().get(commit);
      TestcaseStatistic testcaseStatistic;
      if (commitStatistics != null) {
         testcaseStatistic = commitStatistics.get(test);
      } else {
         testcaseStatistic = null;
      }
      return testcaseStatistic;
   }

   private double getPrintableChangeValue(TestcaseStatistic testcaseStatistic, Change change) {
      double changeValue;
      if (change != null) {
         changeValue = change.getChangePercent();
      } else if (testcaseStatistic != null) {
         changeValue = 0.0;
      } else {
         changeValue = Double.NaN;
      }
      return changeValue;
   }
}