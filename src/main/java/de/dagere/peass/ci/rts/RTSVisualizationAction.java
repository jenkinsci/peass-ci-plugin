package de.dagere.peass.ci.rts;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.Messages;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionCommit;
import io.jenkins.cli.shaded.org.jvnet.localizer.ResourceBundleHolder;

public class RTSVisualizationAction extends VisibleAction {

   private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

   private final TestSelectionConfig config;
   private final Map<String, List<String>> staticSelection;
   private final List<String> dynamicSelection;
   private final String commit, commitOld;
   private final RTSLogSummary logSummary;

   private final CoverageSelectionCommit coverageSelection;
   private final TestSet twiceExecutableTests;

   public RTSVisualizationAction(int id, final TestSelectionConfig config, final Map<String, List<String>> staticSelection, final List<String> dynamicSelection,
         final CoverageSelectionCommit coverageSelection,
         TestSet twiceExecutableTests, final String commit, final String commitOld, final RTSLogSummary logSummary) {
      super(id);
      this.config = config;
      this.staticSelection = staticSelection;
      this.dynamicSelection = dynamicSelection;
      this.coverageSelection = coverageSelection;
      this.twiceExecutableTests = twiceExecutableTests;
      this.commit = commit;
      this.commitOld = commitOld;
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

   public boolean isTwiceExecutable(String testcase) {
      // TODO In general, the action should use TestMethodCall instances to keep type safety; this needs some refactoring
      TestMethodCall test = TestMethodCall.createFromString(testcase);
      if (twiceExecutableTests != null) {
         return twiceExecutableTests.getTestMethods().contains(test);
      } else {
         return true;
      }
   }

   public String getCommit() {
      return commit;
   }

   public String getCommitOld() {
      return commitOld;
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
