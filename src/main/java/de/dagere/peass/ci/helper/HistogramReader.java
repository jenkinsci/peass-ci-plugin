package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.dataloading.ResultLoader;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class HistogramReader {

   private final MeasurementConfig measurementConfig;
   private final File fullResultsFolder;
   private Map<String, MeasurementConfig> updatedConfigurations = new HashMap<>();

   public HistogramReader(final MeasurementConfig measurementConfig, final File fullResultsFolder) {
      this.measurementConfig = measurementConfig;
      this.fullResultsFolder = fullResultsFolder;
   }

   public Map<String, HistogramValues> readMeasurements() {
      final Map<String, HistogramValues> measurements = new TreeMap<>();
      if (fullResultsFolder.exists() && fullResultsFolder.isDirectory()) {
         File[] xmlFiles = fullResultsFolder.listFiles((FileFilter) new WildcardFileFilter("*.json"));
         if (xmlFiles == null) {
            System.out.println("No json-Files were found, measurements is empty!");
            return measurements;
         }

         for (File xmlResultFile : xmlFiles) {
            readFile(measurements, xmlResultFile);
         }
      }
      return measurements;
   }
   
   public Map<String, MeasurementConfig> getUpdatedConfigurations() {
      return updatedConfigurations;
   }

   private void readFile(final Map<String, HistogramValues> measurements, final File xmlResultFile) {
      Kopemedata data = JSONDataLoader.loadData(xmlResultFile);
      // This assumes measurements are only executed once; if this is not the case, the matching result would need to be searched
      final TestMethod testcase = data.getMethods().get(0);
      VMResultChunk chunk = testcase.getDatacollectorResults().get(0).getChunks().get(0);
      
      String testcaseKey = new TestCase(data).toString();
      
      MeasurementConfig currentConfig = getUpdatedConfiguration(testcaseKey, testcase, chunk);
      
      HistogramValues values = loadResults(chunk, currentConfig);
      
      boolean moreThanOneMeasurement = values.getValuesCurrent().length > 1 || values.getValuesBefore().length > 1;
      
      if (moreThanOneMeasurement) {
         measurements.put(testcaseKey, values);
      }
   }

   private HistogramValues loadResults(final VMResultChunk chunk, final MeasurementConfig currentConfig) {
      ResultLoader loader = new ResultLoader(currentConfig);
      loader.loadChunk(chunk);

      HistogramValues values = new HistogramValues(loader.getValsBefore(), loader.getValsAfter(), currentConfig);
      return values;
   }

   private MeasurementConfig getUpdatedConfiguration(final String testcaseKey, final TestMethod testcase, final VMResultChunk chunk) {
      MeasurementConfig currentConfig = new MeasurementConfig(measurementConfig);
      int iterations = (int) MultipleVMTestUtil.getMinIterationCount(chunk.getResults());
      if (iterations != currentConfig.getAllIterations()) {
         currentConfig.setIterations((int) Math.ceil(iterations/2d));
         currentConfig.setWarmup(iterations/2);
      }
      
      currentConfig.setRepetitions((int) MultipleVMTestUtil.getMinRepetitionCount(chunk.getResults()));
      
      if (currentConfig.getAllIterations() != measurementConfig.getAllIterations() ||
            currentConfig.getRepetitions() != measurementConfig.getRepetitions()) {
         updatedConfigurations.put(testcaseKey, currentConfig);
      }
      return currentConfig;
   }
   
   public boolean measurementConfigurationUpdated() {
      return !updatedConfigurations.isEmpty();
   }
}