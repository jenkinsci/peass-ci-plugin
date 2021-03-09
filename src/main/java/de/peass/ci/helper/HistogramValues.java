package de.peass.ci.helper;

import java.util.Arrays;

public class HistogramValues {
   private final double[] valuesCurrent;
   private final double[] valuesBefore;
   
   public HistogramValues(final double[] valuesCurrent, final double[] valuesBefore) {
      this.valuesCurrent = valuesCurrent;
      this.valuesBefore = valuesBefore;
   }

   public double[] getValuesCurrent() {
      return valuesCurrent;
   }
   
   public double[] getValuesBefore() {
      return valuesBefore;
   }

   public String getValuesCurrentReadable() {
      return Arrays.toString(valuesCurrent);
   }

   public String getValuesBeforeReadable() {
      return Arrays.toString(valuesBefore);
   }
}