package de.dagere.peass.ci.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.ci.MeasurementOverviewAction;
import de.dagere.peass.ci.TestConstants;
import de.dagere.peass.config.MeasurementConfig;

class TestHistogramReader {

   private static final File EXAMPLE_DATA_FOLDER = new File(TestConstants.RESOURCE_FOLDER, "demo-results/histogram");

   @Test
   void testHistogramCreation() {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("b02c92af73e3297be617f4c973a7a63fb603565b");
      measurementConfig.getFixedCommitConfig().setCommitOld("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      measurementConfig.setWarmup(2);
      measurementConfig.setIterations(2);
      measurementConfig.setRepetitions(2);

      HistogramReader reader = new HistogramReader(measurementConfig, new File(EXAMPLE_DATA_FOLDER, "b02c92af73e3297be617f4c973a7a63fb603565b"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      double[] valuesBefore = measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesBefore();
      double[] valuesCurrent = measurements.get("de.test.CalleeTest#onlyCallMethod1").getValuesCurrent();

      MeasurementOverviewAction measureVersionActionMock = Mockito.mock(MeasurementOverviewAction.class);
      when(measureVersionActionMock.getValuesReadable(Mockito.any(double[].class))).thenCallRealMethod();

      assertEquals(2, measureVersionActionMock.getValuesReadable(valuesBefore).split(",").length);
      assertEquals(2, measureVersionActionMock.getValuesReadable(valuesCurrent).split(",").length);

      assertFalse(reader.measurementConfigurationUpdated());
   }

   @Test
   void testEmptyHistogram() {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("e80d8a1bf747d1f70dc52260616b36cac9e44561");
      measurementConfig.getFixedCommitConfig().setCommitOld("e80d8a1bf747d1f70dc52260616b36cac9e44561~1");

      HistogramReader reader = new HistogramReader(measurementConfig, new File(EXAMPLE_DATA_FOLDER, "e80d8a1bf747d1f70dc52260616b36cac9e44561"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      assertNull(measurements.get("e80d8a1bf747d1f70dc52260616b36cac9e44561"));

      assertFalse(reader.measurementConfigurationUpdated());
   }

   @Test
   void testUpdatedConfiguration() {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getFixedCommitConfig().setCommit("a23e385264c31def8dcda86c3cf64faa698c62d8");
      measurementConfig.getFixedCommitConfig().setCommitOld("33ce17c04b5218c25c40137d4d09f40fbb3e4f0f");

      HistogramReader reader = new HistogramReader(measurementConfig,
            new File(EXAMPLE_DATA_FOLDER, "measurement_a23e385264c31def8dcda86c3cf64faa698c62d8_33ce17c04b5218c25c40137d4d09f40fbb3e4f0f"));
      Map<String, HistogramValues> measurements = reader.readMeasurements();

      double[] valuesBefore = measurements.get("de.test.CalleeTest#onlyCallMethod2").getValuesBefore();
      double[] valuesCurrent = measurements.get("de.test.CalleeTest#onlyCallMethod2").getValuesCurrent();

      MeasurementOverviewAction measureVersionActionMock = Mockito.mock(MeasurementOverviewAction.class);
      when(measureVersionActionMock.getValuesReadable(Mockito.any(double[].class))).thenCallRealMethod();

      assertEquals(2, measureVersionActionMock.getValuesReadable(valuesBefore).split(",").length);
      assertEquals(2, measureVersionActionMock.getValuesReadable(valuesCurrent).split(",").length);

      assertTrue(reader.measurementConfigurationUpdated());

      MeasurementConfig updatedConfig = reader.getUpdatedConfigurations().get("de.test.CalleeTest#onlyCallMethod2");
      assertEquals(2, updatedConfig.getIterations());
      assertEquals(200, updatedConfig.getRepetitions());
   }
}
