package de.dagere.peass.ci.logs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class LogDisplayAction implements RunAction2 {
   private transient Run<?, ?> run;
   private Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
   private String version;
   private String versionOld;

   public LogDisplayAction(final Map<TestCase, List<LogFiles>> logFiles, final String version, final String versionOld) {
      this.logFiles = logFiles;
      this.version = version;
      this.versionOld = versionOld;
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

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/sd_slower.png";
   }

   @Override
   public String getDisplayName() {
      return "Performance Measurement Logs";
   }

   @Override
   public String getUrlName() {
      return "measurementLogs";
   }

   @Override
   public void onAttached(final Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(final Run<?, ?> run) {
      this.run = run;
   }

}
