package de.dagere.peass.ci.peassOverview;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.peassAnalysis.ChangeLine;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;

public class ProjectData {
   private final StaticTestSelection selection;
   private final ProjectChanges changes;
   
   public ProjectData(StaticTestSelection selection, ProjectChanges changes) {
      this.selection = selection;
      this.changes = changes;
   }

   public StaticTestSelection getSelection() {
      return selection;
   }
   
   public ProjectChanges getChanges() {
      return changes;
   }
   
   public List<ChangeLine> getChangeLines() {
      List<ChangeLine> result = new LinkedList<>();
      String version = selection.getNewestCommit();
      CommitStaticSelection versionStaticSelection = selection.getVersions().get(version);
      if (versionStaticSelection != null) {
         Map<ChangedEntity, TestSet> changedClazzes = versionStaticSelection.getChangedClazzes();
         for (Map.Entry<ChangedEntity, TestSet> entry : changedClazzes.entrySet()) {

            if (entry.getValue().getTests().size() > 0) {
               for (TestCase test : entry.getValue().getTests()) {
                  Change change = changes.getVersion(version).getChange(test);
                  ChangeLine line = new ChangeLine(version, entry.getKey().toString(), test.toString(), change.getChangePercent());
                  result.add(line);
               }
            } else {
               ChangeLine line = new ChangeLine(version, entry.getKey().toString(), "none", 0);
               result.add(line);
            }
         }
      }
      
      return result;
   }
}