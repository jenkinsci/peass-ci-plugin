package de.dagere.peass.ci.helper;

import de.dagere.peass.config.MeasurementConfig;

public class HistogramValues {

   private final double[] valuesBefore;
   private final double[] valuesCurrent;
   private final MeasurementConfig currentConfig;

   public HistogramValues(final double[] valuesBefore, final double[] valuesCurrent, final MeasurementConfig currentConfig) {
      this.valuesBefore = valuesBefore;
      this.valuesCurrent = valuesCurrent;
      this.currentConfig = currentConfig;
   }

   public double[] getValuesBefore() {
      return valuesBefore;
   }

   public double[] getValuesCurrent() {
      return valuesCurrent;
   }

   public MeasurementConfig getCurrentConfig() {
      return currentConfig;
   }
}
