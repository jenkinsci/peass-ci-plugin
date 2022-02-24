package de.dagere.peass.ci.helper;

public class UnitConverter {

   public static final int NANOSECONDS_TO_MICROSECONDS = 1000;
   public static final int NANOSECONDS_TO_MILLISECONDS = 1000 * 1000;
   public static final int NANOSECONDS_TO_SECONDS = 1000 * 1000 * 1000;

   public static final String NANOSECONDS = "ns";
   public static final String MICROSECONDS = "\u00B5s";
   public static final String MILLISECONDS = "ms";
   public static final String SECONDS = "s";

   public static int getFactorByMean(double mean) {
      int count = 0;

      while (count < 3 && mean >= 1000) {
         mean = mean / 1000;
         count++;
      }

      return getFactorByCount(count);
   }

   private static int getFactorByCount(final int count) {
      switch (count) {
      case 0:
         return 1;
      case 1:
         return NANOSECONDS_TO_MICROSECONDS;
      case 2:
         return NANOSECONDS_TO_MILLISECONDS;
      case 3:
         return NANOSECONDS_TO_SECONDS;
      default:
         // this should not happen!
         return 0;
      }
   }

   public static String getUnitByFactor(final int factor) {
      switch (factor) {
      case 1:
         return "ns";
      case NANOSECONDS_TO_MICROSECONDS:
         return "\u00B5s";
      case NANOSECONDS_TO_MILLISECONDS:
         return "ms";
      case NANOSECONDS_TO_SECONDS:
         return "s";
      default:
         // this should not happen!
         return "unknown factor provided!";
      }
   }

}
