package de.peass.ci.helper;

import java.util.Arrays;

public class HistogramValues {
   private double[] valuesCurrent;
   private double[] valuesBefore;
   
   public HistogramValues(double[] valuesCurrent, double[] valuesBefore) {
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