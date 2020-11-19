package de.peass.ci.helper;

import java.io.File;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.ci.ContinuousExecutor;

public class TestHistogramReader {
   
   @Test
   public void testHistogramCreation() throws JAXBException {
      ContinuousExecutor executorMock = Mockito.mock(ContinuousExecutor.class);
      Mockito.when(executorMock.getLatestVersion()).thenReturn("b02c92af73e3297be617f4c973a7a63fb603565b");
      Mockito.when(executorMock.getVersionOld()).thenReturn("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      Mockito.when(executorMock.getFullResultsVersion()).thenReturn(new File("src/test/resources/demo-results/histogram/b02c92af73e3297be617f4c973a7a63fb603565b"));
      
      HistogramReader reader = new HistogramReader(executorMock);
      Map<String, HistogramValues> measurements = reader.readMeasurements();
      
      System.out.println(measurements.keySet());
      
      Assert.assertEquals(measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesBefore().length, 2);
      Assert.assertEquals(measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesCurrent().length, 2);
   }
   
   @Test
   public void testEmptyHistogram() throws JAXBException {
      ContinuousExecutor executorMock = Mockito.mock(ContinuousExecutor.class);
      Mockito.when(executorMock.getLatestVersion()).thenReturn("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      Mockito.when(executorMock.getVersionOld()).thenReturn("e80d8a1bf747d1f70dc52260616b36cac9e44561~1");
      Mockito.when(executorMock.getFullResultsVersion()).thenReturn(new File("src/test/resources/demo-results/histogram/e80d8a1bf747d1f70dc52260616b36cac9e44561"));
      
      HistogramReader reader = new HistogramReader(executorMock);
      Map<String, HistogramValues> measurements = reader.readMeasurements();
   }
}
