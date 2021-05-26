package de.peass.ci.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;

public class TestMeasurementValues {
   private Map<Integer, TestcaseStatistic> statistics = new LinkedHashMap<>();

   public Map<Integer, TestcaseStatistic> getStatistics() {
      return statistics;
   }

   public void setStatistics(final Map<Integer, TestcaseStatistic> statistics) {
      this.statistics = statistics;
   }
   
   
}