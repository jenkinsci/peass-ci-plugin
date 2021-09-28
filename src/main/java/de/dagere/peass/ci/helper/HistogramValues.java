package de.dagere.peass.ci.helper;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.visualization.KoPeMeTreeConverter;

public class HistogramValues {
   
   private static final String NANOSECONDS = "ns";
   private static final String MICROSECONDS = "\u00B5s";
   
   private final double[] valuesCurrent;
   private final double[] valuesBefore;

   private String unit;

   /**
    * Creates histogram values, assuming parameters are in nanoseconds
    */
   public HistogramValues(final double[] valuesCurrent, final double[] valuesBefore, final MeasurementConfiguration currentConfig) {
      double mean = new DescriptiveStatistics(valuesCurrent).getMean();
      int factor;
      if (mean < 1000) {
         unit = NANOSECONDS;
         factor = 1;
      } else {
         unit = MICROSECONDS;
         factor = KoPeMeTreeConverter.NANO_TO_MICRO;
      }
      System.out.println("Unit: " + unit);
      this.valuesCurrent = Arrays.stream(valuesCurrent).map(value -> value / currentConfig.getRepetitions() / factor).toArray();
      this.valuesBefore = Arrays.stream(valuesBefore).map(value -> value / currentConfig.getRepetitions() / factor).toArray();
   }

   /**
    * Returns a javascript visualizable value array in the specified unit
    * 
    * @return A javascript visualizable value array in the specified unit
    */
   public String getValuesCurrentReadable() {
      return Arrays.toString(valuesCurrent);
   }

   public String getValuesBeforeReadable() {
      return Arrays.toString(valuesBefore);
   }
   
   public String getUnit() {
      return unit;
   }
}