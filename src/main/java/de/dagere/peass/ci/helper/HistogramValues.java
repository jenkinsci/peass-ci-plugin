package de.dagere.peass.ci.helper;

import de.dagere.peass.config.MeasurementConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class HistogramValues {

   private final double[] valuesBefore;
   private final double[] valuesCurrent;
   private final MeasurementConfig currentConfig;

   @SuppressFBWarnings
   public HistogramValues(final double[] valuesBefore, final double[] valuesCurrent, final MeasurementConfig currentConfig) {
      this.valuesBefore = valuesBefore;
      this.valuesCurrent = valuesCurrent;
      this.currentConfig = currentConfig;
   }

   @SuppressFBWarnings
   public double[] getValuesBefore() {
      return valuesBefore;
   }

   @SuppressFBWarnings
   public double[] getValuesCurrent() {
      return valuesCurrent;
   }

   public MeasurementConfig getCurrentConfig() {
      return currentConfig;
   }
}
