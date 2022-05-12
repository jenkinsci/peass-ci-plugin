package de.dagere.peass.ci.peassAnalysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.StaticTestSelection;

public class PeassAnalysisAction extends VisibleAction {

   private StaticTestSelection selection;
   private ProjectChanges changes;

   public PeassAnalysisAction(int id, StaticTestSelection selection, ProjectChanges changes) {
      super(id);
      this.selection = selection;
      this.changes = changes;
   }

   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      return "Peass Analysis";
   }

   @Override
   public String getUrlName() {
      return "peassAnalysis_" + id;
   }

   public List<ChangeLine> getChangeLines() {
      List<ChangeLine> result = new LinkedList<>();
      String version = selection.getNewestVersion();
      Map<ChangedEntity, TestSet> changedClazzes = selection.getVersions().get(version).getChangedClazzes();
      for (Map.Entry<ChangedEntity, TestSet> entry : changedClazzes.entrySet()) {
         for (TestCase test : entry.getValue().getTests()) {
            Change change = changes.getVersion(version).getChange(test);
            boolean changeMeasured = (change != null);
            ChangeLine line = new ChangeLine(version, entry.getKey().toString(), test.toString(), changeMeasured);
            result.add(line);
         }

      }
      return result;
   }

   public String getVersion() {
      return selection.getNewestVersion();
   }

   public StaticTestSelection getSelection() {
      return selection;
   }

   public void setSelection(StaticTestSelection selection) {
      this.selection = selection;
   }

   public ProjectChanges getChanges() {
      return changes;
   }

   public void setChanges(ProjectChanges changes) {
      this.changes = changes;
   }

}
