package de.dagere.peass.ci.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TestUnitConverter {

   private static final double NANO_SECOND_MEAN = 123;
   private static final double MICRO_SECOND_MEAN = 1234;
   private static final double MILLI_SECOND_MEAN = 1234567;
   private static final double SECOND_MEAN = 12345678E6;

   @Test
   void testGetFactorByMean() {
      assertEquals(1, UnitConverter.getFactorByMean(NANO_SECOND_MEAN));
      assertEquals(UnitConverter.NANOSECONDS_TO_MICROSECONDS, UnitConverter.getFactorByMean(
          MICRO_SECOND_MEAN));
      assertEquals(UnitConverter.NANOSECONDS_TO_MILLISECONDS, UnitConverter.getFactorByMean(
          MILLI_SECOND_MEAN));
      assertEquals(UnitConverter.NANOSECONDS_TO_SECONDS, UnitConverter.getFactorByMean(SECOND_MEAN));
   }

   @Test
   void testGetUnitByFactor() {
      assertEquals(UnitConverter.NANOSECONDS, UnitConverter.getUnitByFactor(1));
      assertEquals(UnitConverter.MICROSECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_MICROSECONDS));
      assertEquals(UnitConverter.MILLISECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_MILLISECONDS));
      assertEquals(UnitConverter.SECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_SECONDS));
   }

}
