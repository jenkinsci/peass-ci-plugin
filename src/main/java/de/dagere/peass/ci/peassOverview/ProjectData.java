package de.dagere.peass.ci.peassOverview;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

   public boolean isContainsError() {
      return containsError;
   }

   public List<ChangeLine> getChangeLines() {
      List<ChangeLine> result = new LinkedList<>();
      if (selection != null) {
         String commit = selection.getNewestCommit();
         CommitStaticSelection versionStaticSelection = selection.getCommits().get(commit);
         if (versionStaticSelection != null) {
            Map<ChangedEntity, TestSet> changedClazzes = versionStaticSelection.getChangedClazzes();
            for (Map.Entry<ChangedEntity, TestSet> entry : changedClazzes.entrySet()) {

               String changedEntity = entry.getKey().toString();
               if (entry.getValue().getTests().size() > 0) {
                  for (TestCase test : entry.getValue().getTests()) {
                     Map<TestCase, TestcaseStatistic> commitStatistics = statistics.getStatistics().get(commit);
                     TestcaseStatistic testcaseStatistic;
                     if (commitStatistics != null) {
                        testcaseStatistic = commitStatistics.get(test);
                     } else {
                        testcaseStatistic = null;
                     }

                     Changes commitChanges = changes.getCommitChanges(commit);
                     Change change = commitChanges.getChange(test);

                     double changeValue;
                     if (change != null) {
                        changeValue = change.getChangePercent();
                     } else if (testcaseStatistic != null) {
                        changeValue = 0.0;
                     } else {
                        changeValue = Double.NaN;
                     }

                     ChangeLine line = new ChangeLine(commit, changedEntity, test.toString(), changeValue);
                     result.add(line);
                  }
               } else {
                  ChangeLine line = new ChangeLine(commit, changedEntity, "none", Double.NaN);
                  result.add(line);
               }
            }
         }
      }

      return result;
   }
}