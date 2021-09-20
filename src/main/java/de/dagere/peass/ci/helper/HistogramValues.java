package de.dagere.peass.ci.helper;

import java.util.Arrays;

import de.dagere.peass.visualization.KoPeMeTreeConverter;

public class HistogramValues {
   private final double[] valuesCurrent;
   private final double[] valuesBefore;

   /**
    * Creates histogram values, assuming parameters are in nanoseconds
    */
   public HistogramValues(final double[] valuesCurrent, final double[] valuesBefore) {
      this.valuesCurrent = Arrays.stream(valuesCurrent).map(value -> value / KoPeMeTreeConverter.NANO_TO_MICRO).toArray();
      this.valuesBefore = Arrays.stream(valuesBefore).map(value -> value / KoPeMeTreeConverter.NANO_TO_MICRO).toArray();
   }

   /**
    * Returns a javascript visualizable value array in mikroseconds
    * @return A javascript visualizable value array in mikroseconds
    */
   public String getValuesCurrentReadable() {
      return Arrays.toString(valuesCurrent);
   }

   public String getValuesBeforeReadable() {
      return Arrays.toString(valuesBefore);
   }
}