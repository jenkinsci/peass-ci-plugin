package de.dagere.peass.ci;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.ci.helper.RCAVisualizer;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class MeasureVersionAction extends VisibleAction {

   private static final Logger LOG = LogManager.getLogger(MeasureVersionAction.class);

   private MeasurementConfig config;
   private Changes changes;
   private ProjectStatistics statistics;
   private final Map<String, TestcaseStatistic> noWarmupStatistics;
   private Map<String, HistogramValues> measurements;
   private String prefix;
   private Map<String, MeasurementConfig> updatedConfigurations;

   public MeasureVersionAction(final MeasurementConfig config, final Changes changes, final ProjectStatistics statistics,
         final Map<String, TestcaseStatistic> noWarmupStatistics, final Map<String, HistogramValues> measurements,
         final Map<String, MeasurementConfig> updatedConfigurations) {
      this.config = config;
      this.changes = changes;
      this.statistics = statistics;
      this.noWarmupStatistics = noWarmupStatistics;
      this.measurements = measurements;
      this.updatedConfigurations = updatedConfigurations;
      for (Entry<String, List<Change>> change : changes.getTestcaseChanges().entrySet()) {
         System.out.println(change.getKey());
      }
      prefix = RCAVisualizer.getLongestPrefix(measurements.keySet());
      LOG.debug("Prefix: {} Keys: {}", prefix, measurements.keySet());
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

   public MeasurementConfig getConfig() {
      return config;
   }

   public boolean hasUpdatedConfigurations() {
      return !updatedConfigurations.isEmpty();
   }

   public Map<String, MeasurementConfig> getUpdatedConfigurations() {
      return updatedConfigurations;
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

   public double getCriticalTValue() {
      int degreesOfFreedom = getDegreesOfFreedom();
      return StatisticUtil.getCriticalValueTTest(config.getStatisticsConfig().getType1error(), degreesOfFreedom);
   }

   public int getDegreesOfFreedom() {
      int degreesOfFreedom = config.getVms() * 2 - 2;
      return degreesOfFreedom;
   }

   public double abs(final double value) {
      return Math.abs(value);
   }

   public TestcaseStatistic getTestcaseStatistic(final String testcase) {
      Entry<String, Map<TestCase, TestcaseStatistic>> testcaseStatisticEntry = statistics.getStatistics().entrySet().iterator().next();
      Map<TestCase, TestcaseStatistic> testcaseStatistic = testcaseStatisticEntry.getValue();
      return testcaseStatistic.get(new TestCase(testcase));
   }

   public TestcaseStatistic getNoWarmupStatistic(final String testcase) {
      return noWarmupStatistics.get(testcase);
   }

   public String getReducedName(final String name) {
      return name.substring(prefix.length());
   }

   public double round(final double value) {
      return Math.round(value * 100) / 100d;
   }
}
