package de.dagere.peass.ci.logs.measurement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.logs.LogFiles;

public class LogOverviewAction extends VisibleAction {
   
   private Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
   private String commit;
   private String commitOld;
   private int vms;
   private boolean redirectSubprocessOutputToFile;

   public LogOverviewAction(int id, final Map<TestCase, List<LogFiles>> logFiles, final String commit, final String commitOld, int vms, final boolean redirectSubprocessOutputToFile) {
      super(id);
      this.logFiles = logFiles;
      this.commit = commit;
      this.commitOld = commitOld;
      this.vms = vms;
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
   }
   
   public Map<TestCase, List<LogFiles>> getLogFiles() {
      return logFiles;
   }
   
   public String getVersion() {
      return commit;
   }
   
   public String getVersionOld() {
      return commitOld;
   }
   
   public int getVms() {
      return vms;
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
      return Messages.LogOverviewAction_DisplayName();
   }

   @Override
   public String getUrlName() {
      return "measurementLogOverview_" + id;
   }


}
