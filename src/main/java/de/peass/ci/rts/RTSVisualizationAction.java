package de.peass.ci.rts;

import java.util.List;
import java.util.Map;

import de.dagere.peass.config.DependencyConfig;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class RTSVisualizationAction implements RunAction2 {
   private transient Run<?, ?> run;

   private final DependencyConfig config;
   private final Map<String, List<String>> staticSelection;
   private final List<String> dynamicSelection;

   public RTSVisualizationAction(final DependencyConfig config, final Map<String, List<String>> staticSelection, final List<String> dynamicSelection) {
      this.config = config;
      this.staticSelection = staticSelection;
      this.dynamicSelection = dynamicSelection;
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

   public String getCoveragebasedSelection() {
      return null;
   }

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/rca.png";
   }

   @Override
   public String getDisplayName() {
      return "Regression Test Results";
   }

   @Override
   public String getUrlName() {
      return "rtsResults";
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
