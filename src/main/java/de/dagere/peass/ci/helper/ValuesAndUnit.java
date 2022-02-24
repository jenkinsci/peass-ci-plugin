package de.dagere.peass.ci.helper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ValuesAndUnit {

   private final double[] values;
   private final String unit;

   @SuppressFBWarnings
   public ValuesAndUnit(final double[] values, final String unit) {
      this.values = values;
      this.unit = unit;
   }

   @SuppressFBWarnings
   public double[] getValues() {
      return values;
   }

   public String getUnit() {
      return unit;
   }

}
