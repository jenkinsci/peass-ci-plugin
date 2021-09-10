package de.dagere.peass.ci.rts;

import java.util.List;
import java.util.Map;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionVersion;

public class RTSVisualizationAction extends VisibleAction {

   private final DependencyConfig config;
   private final Map<String, List<String>> staticSelection;
   private final List<String> dynamicSelection;
   
   //TODO Display count of calls for each test
   private final CoverageSelectionVersion coverageSelection;

   public RTSVisualizationAction(final DependencyConfig config, final Map<String, List<String>> staticSelection, final List<String> dynamicSelection, final CoverageSelectionVersion coverageSelection) {
      this.config = config;
      this.staticSelection = staticSelection;
      this.dynamicSelection = dynamicSelection;
      this.coverageSelection = coverageSelection;
   }
   
   public DependencyConfig getConfig() {
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

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/rts.jpg";
   }

   @Override
   public String getDisplayName() {
      return "Regression Test Selection Results";
   }

   @Override
   public String getUrlName() {
      return "rtsResults";
   }
}
