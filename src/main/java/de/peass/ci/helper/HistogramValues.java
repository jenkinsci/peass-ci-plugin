package de.peass.ci.helper;

import java.util.Arrays;

public class HistogramValues {
   double[] valuesBefore;
   double[] valuesCurrent;

   public HistogramValues(double[] valuesCurrent, double[] valuesBefore) {
      this.valuesBefore = valuesBefore;
      this.valuesCurrent = valuesCurrent;
   }

   public double[] getValuesBefore() {
      return valuesBefore;
   }

   public double[] getValuesCurrent() {
      return valuesCurrent;
   }

   public String getValuesCurrentReadable() {
      return Arrays.toString(valuesCurrent);
   }

   public String getValuesBeforeReadable() {
      return Arrays.toString(valuesBefore);
   }
}