package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.measurement.analysis.MultipleVMTestUtil;
import de.dagere.peass.measurement.analysis.ResultLoader;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class HistogramReader {
   private static final int MIKRO = 1000;

   private final MeasurementConfiguration measurementConfig;
   private final File fullResultsFolder;
   private Map<String, MeasurementConfiguration> updatedConfigurations = new HashMap<>();

   public HistogramReader(final MeasurementConfiguration measurementConfig, final File fullResultsFolder) {
      this.measurementConfig = measurementConfig;
      this.fullResultsFolder = fullResultsFolder;
   }

   public Map<String, HistogramValues> readMeasurements() throws JAXBException {
      final Map<String, HistogramValues> measurements = new TreeMap<>();
      if (fullResultsFolder.exists() && fullResultsFolder.isDirectory()) {
         File[] xmlFiles = fullResultsFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"));
         if (xmlFiles == null) {
            System.out.println("No xml-Files were found, measurements is empty!");
            return measurements;
         }

         for (File xmlResultFile : xmlFiles) {
            readFile(measurements, xmlResultFile);
         }
      }
      return measurements;
   }
   
   public Map<String, MeasurementConfiguration> getUpdatedConfigurations() {
      return updatedConfigurations;
   }

   private void readFile(final Map<String, HistogramValues> measurements, final File xmlResultFile) throws JAXBException {
      Kopemedata data = XMLDataLoader.loadData(xmlResultFile);
      // This assumes measurements are only executed once; if this is not the case, the matching result would need to be searched
      final TestcaseType testcase = data.getTestcases().getTestcase().get(0);
      Chunk chunk = testcase.getDatacollector().get(0).getChunk().get(0);
      String testcaseKey = data.getTestcases().getClazz() + "#" + testcase.getName();
      
      MeasurementConfiguration currentConfig = getUpdatedConfiguration(testcaseKey, testcase, chunk);
      
      HistogramValues values = loadResults(chunk, currentConfig);
        
      measurements.put(testcaseKey, values);
   }

   private HistogramValues loadResults(final Chunk chunk, final MeasurementConfiguration currentConfig) {
      ResultLoader loader = new ResultLoader(currentConfig, null, null, 0);
      loader.loadChunk(chunk);

      HistogramValues values = new HistogramValues(loader.getValsAfter(), loader.getValsBefore());
      return values;
   }

   private MeasurementConfiguration getUpdatedConfiguration(final String testcaseKey, final TestcaseType testcase, final Chunk chunk) {
      MeasurementConfiguration currentConfig = new MeasurementConfiguration(measurementConfig);
      currentConfig.setIterations((int) MultipleVMTestUtil.getMinIterationCount(chunk.getResult()));
      currentConfig.setRepetitions((int) MultipleVMTestUtil.getMinRepetitionCount(chunk.getResult()));
      
      if (currentConfig.getIterations() != measurementConfig.getIterations() ||
            currentConfig.getRepetitions() != measurementConfig.getRepetitions()) {
         updatedConfigurations.put(testcaseKey, currentConfig);
      }
      return currentConfig;
   }
   
   public boolean measurementConfigurationUpdated() {
      return !updatedConfigurations.isEmpty();
   }
}