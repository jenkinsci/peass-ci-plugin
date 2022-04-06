package de.peass.ci.helper;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.ci.MeasureVersionAction;
import de.dagere.peass.ci.helper.HistogramReader;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.config.MeasurementConfig;

public class TestHistogramReader {

   private static final File EXAMPLE_DATA_FOLDER = new File("src/test/resources/demo-results/histogram");

   @Test
   public void testHistogramCreation() throws JAXBException {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setCommit("b02c92af73e3297be617f4c973a7a63fb603565b");
      measurementConfig.getExecutionConfig().setCommitOld("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      measurementConfig.setWarmup(2);
      measurementConfig.setIterations(2);
      measurementConfig.setRepetitions(2);

      HistogramReader reader = new HistogramReader(measurementConfig, new File(EXAMPLE_DATA_FOLDER, "b02c92af73e3297be617f4c973a7a63fb603565b"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      double[] valuesBefore = measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesBefore();
      double[] valuesCurrent = measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesCurrent();

      MeasureVersionAction measureVersionActionMock = Mockito.mock(MeasureVersionAction.class);
      when(measureVersionActionMock.getValuesReadable(any(double[].class))).thenCallRealMethod();

      Assert.assertEquals(2, measureVersionActionMock.getValuesReadable(valuesBefore).split(",").length);
      Assert.assertEquals(2, measureVersionActionMock.getValuesReadable(valuesCurrent).split(",").length);

      Assert.assertFalse(reader.measurementConfigurationUpdated());
   }

   @Test
   public void testEmptyHistogram() throws JAXBException {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setCommit("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      measurementConfig.getExecutionConfig().setCommitOld("e80d8a1bf747d1f70dc52260616b36cac9e44561~1");

      HistogramReader reader = new HistogramReader(measurementConfig, new File(EXAMPLE_DATA_FOLDER, "e80d8a1bf747d1f70dc52260616b36cac9e44561"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      Assert.assertNull(measurements.get("e80d8a1bf747d1f70dc52260616b36cac9e44561"));

      Assert.assertFalse(reader.measurementConfigurationUpdated());
   }

   @Test
   public void testUpdatedConfiguration() throws JAXBException {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setCommit("a23e385264c31def8dcda86c3cf64faa698c62d8");
      measurementConfig.getExecutionConfig().setCommitOld("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f");

      HistogramReader reader = new HistogramReader(measurementConfig,
            new File(EXAMPLE_DATA_FOLDER, "measurement_a23e385264c31def8dcda86c3cf64faa698c62d8_33ce17c04b5218c25c40137d4d09f40fbb3e4f0f"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      double[] valuesBefore = measurements.get("de.test.CalleeTest#onlyCallMethod2").getValuesBefore();
      double[] valuesCurrent = measurements.get("de.test.CalleeTest#onlyCallMethod2").getValuesCurrent();

      MeasureVersionAction measureVersionActionMock = Mockito.mock(MeasureVersionAction.class);
      when(measureVersionActionMock.getValuesReadable(any(double[].class))).thenCallRealMethod();

      Assert.assertEquals(measureVersionActionMock.getValuesReadable(valuesBefore).split(",").length, 2);
      Assert.assertEquals(measureVersionActionMock.getValuesReadable(valuesCurrent).split(",").length, 2);

      Assert.assertTrue(reader.measurementConfigurationUpdated());

      MeasurementConfig updatedConfig = reader.getUpdatedConfigurations().get("de.test.CalleeTest#onlyCallMethod2");
      Assert.assertEquals(updatedConfig.getIterations(), 2);
      Assert.assertEquals(updatedConfig.getRepetitions(), 200);
   }
}
