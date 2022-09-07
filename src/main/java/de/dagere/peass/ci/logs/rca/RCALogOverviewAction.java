package de.dagere.peass.ci.logs.rca;

import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class RCALogOverviewAction extends VisibleAction {
   
   private final Map<TestCase, List<RCALevel>> testLevelMap;
   private String commit;
   private String commitOld;
   private boolean redirectSubprocessOutputToFile;

   public RCALogOverviewAction(int id, final Map<TestCase, List<RCALevel>> testLevelMap, final String commit, final String commitOld, final boolean redirectSubprocessOutputToFile) {
      super(id);
      this.testLevelMap = testLevelMap;
      this.commit = commit;
      this.commitOld = commitOld;
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
   }

   public Map<TestCase, List<RCALevel>> getTestLevelMap() {
      return testLevelMap;
   }

   public String getVersion() {
      return commit;
   }

   public String getVersionOld() {
      return commitOld;
   }

   public boolean isRedirectSubprocessOutputToFile(){
      return redirectSubprocessOutputToFile;
   }

   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      return Messages.RCALogOverviewAction_DisplayName();
   }

   @Override
   public String getUrlName() {
      return "rcaLogOverview_" + id;
   }
}
