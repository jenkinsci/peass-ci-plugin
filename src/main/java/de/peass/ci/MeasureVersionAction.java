package de.peass.ci;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.ci.helper.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class MeasureVersionAction implements RunAction2 {

   private transient Run<?, ?> run;
   
   private MeasurementConfiguration config;
   private Changes changes;
   private ProjectStatistics statistics;
   private Map<String, HistogramValues> measurements;
   private String prefix;

   public MeasureVersionAction(final MeasurementConfiguration config, final Changes changes, final ProjectStatistics statistics, final Map<String, HistogramValues> measurements) {
      this.config = config;
      this.changes = changes;
      this.statistics = statistics;
      this.measurements = measurements;
      for (Entry<String, List<Change>> change : changes.getTestcaseChanges().entrySet()) {
         System.out.println(change.getKey());
      }
      prefix = RCAVisualizer.getLongestPrefix(changes);
   }

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/sd_slower.png";
   }

   @Override
   public String getDisplayName() {
      return "Performance Measurement";
   }

   @Override
   public String getUrlName() {
      return "measurement";
   }

   public MeasurementConfiguration getConfig() {
      return config;
   }

   public ProjectStatistics getStatistics() {
      return statistics;
   }

   public Changes getChanges() {
      return changes;
   }
   
   public boolean testIsChanged(final String testcase) {
      boolean isChanged = false;
      for (Entry<String, List<Change>> changeEntry : changes.getTestcaseChanges().entrySet()) {
         for (Change change : changeEntry.getValue()) {
            final String changedTestcase = changeEntry.getKey() + "#" + change.getMethod();
            if (testcase.equals(changedTestcase)) {
               isChanged = true;
            }
         }
      }
      return isChanged;
   }
   
   public Map<String, HistogramValues> getMeasurements() {
      return measurements;
   }
   
   public String getReducedName(final String name) {
      return name.substring(prefix.length()+1);
   }

   @Override
   public void onAttached(final Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(final Run<?, ?> run) {
      this.run = run;
   }

   public Run<?, ?> getRun() {
      return run;
   }
   
   public double round(final double value) {
      return Math.round(value*100)/100d;
   }
}
