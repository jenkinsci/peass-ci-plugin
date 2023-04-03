package de.dagere.peass.ci.peassOverview;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class ProjectData {
   private final StaticTestSelection selection;
   private final ExecutionData executionData, twiceExecutabilitySelection, coverageSelection;
   private final ProjectChanges changes;
   private final ProjectStatistics statistics;
   private final boolean containsError;

   public ProjectData(StaticTestSelection selection, ExecutionData executionData, ExecutionData twiceExecutabilitySelection, ExecutionData coverageSelection,
         ProjectChanges changes, ProjectStatistics statistics, boolean containsError) {
      this.selection = selection;
      this.executionData = executionData;
      this.twiceExecutabilitySelection = twiceExecutabilitySelection;
      this.coverageSelection = coverageSelection;
      this.changes = changes;
      this.statistics = statistics;
      this.containsError = containsError;
   }

   /**
    * @deprecated only present to keep existing overviews working; will be removed on next release
    */
   public ProjectData(StaticTestSelection selection, ProjectChanges changes, ProjectStatistics statistics, boolean containsError) {
      this.selection = selection;
      this.executionData = null;
      this.twiceExecutabilitySelection = null;
      this.coverageSelection = null;
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
            if (commitStaticSelection != null) {
               addCommitData(result, commit, commitStaticSelection);
            }
         }
      }

      return result;
   }

   private void addCommitData(List<ChangeLine> result, String commit, CommitStaticSelection commitStaticSelection) {
      Map<Type, TestSet> changedClazzes = commitStaticSelection.getChangedClazzes();

      Map<TestMethodCall, Set<String>> oneTestCausingChanges = new LinkedHashMap<>();
      List<String> changesWithNoTest = new LinkedList<>();
      fillOneOrLessTests(changedClazzes, oneTestCausingChanges, changesWithNoTest);

      addOneTestCausingChanges(result, commit, oneTestCausingChanges);

      addNoneTestCausingChanges(result, commit, changesWithNoTest);
   }

   private void fillOneOrLessTests(Map<Type, TestSet> changedClazzes, Map<TestMethodCall, Set<String>> oneTestCausingChanges, List<String> changesWithNoTest) {
      for (Map.Entry<Type, TestSet> entry : changedClazzes.entrySet()) {

         String changedEntity = entry.getKey().toString();
         if (entry.getValue().getTestMethods().size() > 0) {
            for (TestMethodCall test : entry.getValue().getTestMethods()) {
               if (!oneTestCausingChanges.containsKey(test)) {
                  oneTestCausingChanges.put(test, new TreeSet<>());
               }
               oneTestCausingChanges.get(test).add(changedEntity);
            }
         } else if (entry.getValue().getTestMethods().size() == 1) {
            TestMethodCall test = entry.getValue().getTestMethods().iterator().next();
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
         ChangeLine line = new ChangeLine(commit, changesWithNoTest, "none", Double.NaN, null);
         result.add(line);
      }
   }

   private void addOneTestCausingChanges(List<ChangeLine> result, String commit, Map<TestMethodCall, Set<String>> oneTestCausingChanges) {
      for (Map.Entry<TestMethodCall, Set<String>> oneTestEntry : oneTestCausingChanges.entrySet()) {
         TestMethodCall test = oneTestEntry.getKey();

         double changeValue = getChangeValue(commit, test);

         List<String> changesAsList = new LinkedList<>();
         changesAsList.addAll(oneTestEntry.getValue());

         DynamicallyUnselected dynamicallyUnselectedReason = getUnselectedReason(commit, test);
         System.out.println("Test " + test + " dynamically unselected due to " + dynamicallyUnselectedReason);

         ChangeLine line = new ChangeLine(commit, changesAsList, test.toString(), changeValue, dynamicallyUnselectedReason);
         result.add(line);
      }
   }

   private DynamicallyUnselected getUnselectedReason(String commit, TestMethodCall test) {
      DynamicallyUnselected dynamicallyUnselectedReason = null;
      if (coverageSelection != null) {
         System.out.println("Checking coverage selection");
         boolean selected = coverageSelection.commitContainsTest(commit, test);
         if (!selected) {
            dynamicallyUnselectedReason = DynamicallyUnselected.COVERAGE_SELECTION;
         }
      } else if (twiceExecutabilitySelection != null) {
         boolean selected = twiceExecutabilitySelection.commitContainsTest(commit, test);
         if (!selected) {
            dynamicallyUnselectedReason = DynamicallyUnselected.TWICE_EXECUTABILITY;
         }
      } else if (executionData != null) {
         boolean selected = executionData.commitContainsTest(commit, test);
         if (!selected) {
            dynamicallyUnselectedReason = DynamicallyUnselected.TRACE;
         }
      }
      return dynamicallyUnselectedReason;
   }

   private double getChangeValue(String commit, TestMethodCall test) {
      TestcaseStatistic testcaseStatistic = getTestcaseStatistic(commit, test);

      Changes commitChanges = changes.getCommitChanges(commit);
      Change change = commitChanges.getChange(test);

      double changeValue = getPrintableChangeValue(testcaseStatistic, change);
      return changeValue;
   }

   private TestcaseStatistic getTestcaseStatistic(String commit, TestCase test) {
      Map<TestMethodCall, TestcaseStatistic> commitStatistics = statistics.getStatistics().get(commit);
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