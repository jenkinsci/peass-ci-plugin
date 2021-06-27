package de.peass.ci.helper;

import java.io.File;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.ci.helper.HistogramReader;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.config.MeasurementConfiguration;

public class TestHistogramReader {
   
   @Test
   public void testHistogramCreation() throws JAXBException {
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      measurementConfig.setVersion("b02c92af73e3297be617f4c973a7a63fb603565b");
      measurementConfig.setVersionOld("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      
      HistogramReader reader = new HistogramReader(measurementConfig, new File("src/test/resources/demo-results/histogram/b02c92af73e3297be617f4c973a7a63fb603565b"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();
      
      System.out.println(measurements.keySet());
      
      Assert.assertEquals(measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesBeforeReadable().split(",").length, 2);
      Assert.assertEquals(measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesBeforeReadable().split(",").length, 2);
   }
   
   @Test
   public void testEmptyHistogram() throws JAXBException {
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      measurementConfig.setVersion("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      measurementConfig.setVersionOld("e80d8a1bf747d1f70dc52260616b36cac9e44561~1");
      
      HistogramReader reader = new HistogramReader(measurementConfig, new File("src/test/resources/demo-results/histogram/e80d8a1bf747d1f70dc52260616b36cac9e44561"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();
      
      Assert.assertNull(measurements.get("e80d8a1bf747d1f70dc52260616b36cac9e44561"));
   }
}
