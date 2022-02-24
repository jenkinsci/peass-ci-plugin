package de.peass.ci.helper;

import de.dagere.peass.ci.helper.UnitConverter;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestUnitConverter {

   final double nanoSecondMean = 123;
   final double mikroSecondMean = 1234;
   final double milliSecondMean = 1234567;
   final double secondMean = 12345678E6;

   @Test
   public void testGetFactorByMean() {
      Assert.assertEquals(1, UnitConverter.getFactorByMean(nanoSecondMean));
      Assert.assertEquals(UnitConverter.NANOSECONDS_TO_MICROSECONDS, UnitConverter.getFactorByMean(mikroSecondMean));
      Assert.assertEquals(UnitConverter.NANOSECONDS_TO_MILLISECONDS, UnitConverter.getFactorByMean(milliSecondMean));
      Assert.assertEquals(UnitConverter.NANOSECONDS_TO_SECONDS, UnitConverter.getFactorByMean(secondMean));
   }

   @Test
   public void testGetUnitByFactor() {
      Assert.assertEquals(UnitConverter.NANOSECONDS, UnitConverter.getUnitByFactor(1));
      Assert.assertEquals(UnitConverter.MICROSECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_MICROSECONDS));
      Assert.assertEquals(UnitConverter.MILLISECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_MILLISECONDS));
      Assert.assertEquals(UnitConverter.SECONDS, UnitConverter.getUnitByFactor(UnitConverter.NANOSECONDS_TO_SECONDS));
   }

}
