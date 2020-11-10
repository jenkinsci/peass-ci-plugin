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
import de.peass.ci.ContinuousExecutor;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class HistogramReader{
   final ContinuousExecutor executor;

   public HistogramReader(ContinuousExecutor executor) {
      this.executor = executor;
   }
   
   public Map<String, HistogramValues> readMeasurements() throws JAXBException {
      Map<String, HistogramValues> measurements = new TreeMap<>(); 
      File measurementsFullFolder = executor.getFullResultsVersion();
      for (File xmlResultFile : measurementsFullFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
         Kopemedata data = XMLDataLoader.loadData(xmlResultFile);
         // This assumes measurements are only executed once; if this is not the case, the matching result would need to be searched
         final TestcaseType testcase = data.getTestcases().getTestcase().get(0);
         HistogramValues values = getHistogramValues(testcase);
         measurements.put(data.getTestcases().getClazz() + "#" + testcase.getName(), values);
      }
      return measurements;
   }

   private HistogramValues getHistogramValues(final TestcaseType testcase) {
      Chunk chunk = testcase.getDatacollector().get(0).getChunk().get(0);

      List<Double> old = new LinkedList<>();
      List<Double> current = new LinkedList<>();
      for (Result result : chunk.getResult()) {
         if (result.getVersion().getGitversion().equals(executor.getVersionOld())) {
            old.add(result.getValue());
         }
         if (result.getVersion().getGitversion().equals(executor.getLatestVersion())) {
            current.add(result.getValue());
         }
      }
      HistogramValues values = new HistogramValues(current.stream().mapToDouble(i -> i).toArray(),
            old.stream().mapToDouble(i -> i).toArray());
      return values;
   }
}