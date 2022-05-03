package de.dagere.peass.ci.logs.measurement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.logs.LogFiles;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class LogOverviewAction extends VisibleAction {
   
   private Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
   private String version;
   private String versionOld;
   private int vms;
   private boolean redirectSubprocessOutputToFile;

   public LogOverviewAction(final Map<TestCase, List<LogFiles>> logFiles, final String version, final String versionOld, int vms, final boolean redirectSubprocessOutputToFile) {
      this.logFiles = logFiles;
      this.version = version;
      this.versionOld = versionOld;
      this.vms = vms;
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
   }
   
   public Map<TestCase, List<LogFiles>> getLogFiles() {
      return logFiles;
   }
   
   public String getVersion() {
      return version;
   }
   
   public String getVersionOld() {
      return versionOld;
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
