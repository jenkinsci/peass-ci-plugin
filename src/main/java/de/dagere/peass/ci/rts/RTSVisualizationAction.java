package de.dagere.peass.ci.rts;

import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionVersion;
import io.jenkins.cli.shaded.org.jvnet.localizer.ResourceBundleHolder;

public class RTSVisualizationAction extends VisibleAction {

   private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class); 
   
   private final TestSelectionConfig config;
   private final Map<String, List<String>> staticSelection;
   private final List<String> dynamicSelection;
   private final String version, versionOld;
   private final RTSLogSummary logSummary;
   
   //TODO Display count of calls for each test
   private final CoverageSelectionVersion coverageSelection;

   public RTSVisualizationAction(final TestSelectionConfig config, final Map<String, List<String>> staticSelection, final List<String> dynamicSelection, final CoverageSelectionVersion coverageSelection,
         final String version, final String versionOld, final RTSLogSummary logSummary) {
      this.config = config;
      this.staticSelection = staticSelection;
      this.dynamicSelection = dynamicSelection;
      this.coverageSelection = coverageSelection;
      this.version = version;
      this.versionOld = versionOld;
      this.logSummary = logSummary;
   }
   
   public TestSelectionConfig getConfig() {
      return config;
   }

   public Map<String, List<String>> getStaticSelection() {
      return staticSelection;
   }

   public List<String> getDynamicSelection() {
      return dynamicSelection;
   }

   public CoverageSelectionVersion getCoveragebasedSelection() {
      return coverageSelection;
   }
   
   public String getVersion() {
      return version;
   }
   
   public String getVersionOld() {
      return versionOld;
   }
   
   public RTSLogSummary getLogSummary() {
      return logSummary;
   }


   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/rts.png";
   }

   @Override
   public String getDisplayName() {
      return Messages.RTSVisualizationAction_DisplayName();
   }

   @Override
   public String getUrlName() {
      return "rtsResults_" + id;
   }
}
