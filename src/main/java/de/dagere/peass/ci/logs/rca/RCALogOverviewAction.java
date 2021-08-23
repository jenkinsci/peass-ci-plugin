package de.dagere.peass.ci.logs.rca;

import java.util.List;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class RCALogOverviewAction implements RunAction2 {
   
   private transient Run<?, ?> run;
   private final Map<TestCase, List<RCALevel>> testLevelMap;
   private String version;
   private String versionOld;

   public RCALogOverviewAction(final Map<TestCase, List<RCALevel>> testLevelMap, final String version, final String versionOld) {
      this.testLevelMap = testLevelMap;
      this.version = version;
      this.versionOld = versionOld;
   }

   public Map<TestCase, List<RCALevel>> getTestLevelMap() {
      return testLevelMap;
   }

   public String getVersion() {
      return version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   @Override
   public String getIconFileName() {
      return "notepad.png";
   }

   @Override
   public String getDisplayName() {
      // TODO Auto-generated method stub
      return "Root Cause Analysis Log Overview";
   }

   @Override
   public String getUrlName() {
      return "rcaLogOverview";
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
