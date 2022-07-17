package de.dagere.peass.ci.rts;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionCommit;
import io.jenkins.cli.shaded.org.jvnet.localizer.ResourceBundleHolder;

public class RTSVisualizationAction extends VisibleAction {

   private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

   private final TestSelectionConfig config;
   private final Map<String, List<String>> staticSelection;
   private final List<String> dynamicSelection;
   private final String version, versionOld;
   private final RTSLogSummary logSummary;

   private final CoverageSelectionCommit coverageSelection;

   public RTSVisualizationAction(int id, final TestSelectionConfig config, final Map<String, List<String>> staticSelection, final List<String> dynamicSelection,
         final CoverageSelectionCommit coverageSelection,
         final String version, final String versionOld, final RTSLogSummary logSummary) {
      super(id);
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

   public Map<List<String>, List<List<String>>> getStaticSelectionPrintable() {
      Map<List<String>, List<List<String>>> printableSelection = new LinkedHashMap<>();
      for (Map.Entry<String, List<String>> originalSelectionEntry : staticSelection.entrySet()) {
         List<String> key = LineUtil.createPrintable(originalSelectionEntry.getKey());
         List<List<String>> testcases = new LinkedList<>();
         for (String originalTestcase : originalSelectionEntry.getValue()) {
            List<String> printableTestcase = LineUtil.createPrintable(originalTestcase);
            testcases.add(printableTestcase);
         }

         printableSelection.put(key, testcases);
      }
      return printableSelection;

   }


   public List<String> getDynamicSelection() {
      return dynamicSelection;
   }

   public CoverageSelectionCommit getCoveragebasedSelection() {
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
