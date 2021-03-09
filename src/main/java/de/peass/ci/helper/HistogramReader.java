package de.peass.ci.helper;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.config.MeasurementConfiguration;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class HistogramReader {
   private static final int MIKRO = 1000;

   private final MeasurementConfiguration measurementConfig;
   private final File fullResultsFolder;

   public HistogramReader(final MeasurementConfiguration measurementConfig, final File fullResultsFolder) {
      this.measurementConfig = measurementConfig;
      this.fullResultsFolder = fullResultsFolder;
   }

   public Map<String, HistogramValues> readMeasurements() throws JAXBException {
      final Map<String, HistogramValues> measurements = new TreeMap<>();
      if (fullResultsFolder.exists() && fullResultsFolder.isDirectory()) {
         for (File xmlResultFile : fullResultsFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
            Kopemedata data = XMLDataLoader.loadData(xmlResultFile);
            // This assumes measurements are only executed once; if this is not the case, the matching result would need to be searched
            final TestcaseType testcase = data.getTestcases().getTestcase().get(0);
            final HistogramValues values = getHistogramValues(testcase);
            measurements.put(data.getTestcases().getClazz() + "#" + testcase.getName(), values);
         }
      }
      return measurements;
   }

   private HistogramValues getHistogramValues(final TestcaseType testcase) {
      Chunk chunk = testcase.getDatacollector().get(0).getChunk().get(0);

      final List<Double> current = new LinkedList<>();
      final List<Double> old = new LinkedList<>();

      for (Result result : chunk.getResult()) {
         final double singleRepetitionValue = result.getValue() / result.getRepetitions() / MIKRO;
         if (result.getVersion().getGitversion().equals(measurementConfig.getVersion())) {
            current.add(singleRepetitionValue);
         }
         if (result.getVersion().getGitversion().equals(measurementConfig.getVersionOld())) {
            old.add(singleRepetitionValue);
         }

      }
      HistogramValues values = new HistogramValues(current, old);
      return values;
   }
}