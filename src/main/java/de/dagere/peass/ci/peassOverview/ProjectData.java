package de.dagere.peass.ci.peassOverview;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;

public class ProjectData {
   private final StaticTestSelection selection;
   private final ProjectChanges changes;
   private final boolean containsError;

   public ProjectData(StaticTestSelection selection, ProjectChanges changes, boolean containsError) {
      this.selection = selection;
      this.changes = changes;
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

               if (entry.getValue().getTests().size() > 0) {
                  for (TestCase test : entry.getValue().getTests()) {
                     Change change = changes.getCommitChanges(commit).getChange(test);
                     ChangeLine line = new ChangeLine(commit, entry.getKey().toString(), test.toString(), change.getChangePercent());
                     result.add(line);
                  }
               } else {
                  ChangeLine line = new ChangeLine(commit, entry.getKey().toString(), "none", 0);
                  result.add(line);
               }
            }
         }
      }

      return result;
   }
}