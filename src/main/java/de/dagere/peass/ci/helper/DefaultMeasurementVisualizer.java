package de.dagere.peass.ci.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.ci.MeasurementVisualizationAction;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.visualization.GraphNode;
import de.dagere.peass.visualization.KoPeMeTreeConverter;
import hudson.model.Run;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class DefaultMeasurementVisualizer {
   private static final Logger LOG = LogManager.getLogger(DefaultMeasurementVisualizer.class);

   private final File dataFolder;
   private final String version;
   private final Run<?, ?> run;
   private final VisualizationFolderManager visualizationFolders;
   private final Set<String> tests;
   private final Map<String, TestcaseStatistic> noWarmupStatistics = new HashMap<>();

   public DefaultMeasurementVisualizer(final File dataFolder, final String version, final Run<?, ?> run, final VisualizationFolderManager visualizationFolders,
         final Set<String> tests) {
      this.dataFolder = dataFolder;
      this.version = version;
      this.run = run;
      this.visualizationFolders = visualizationFolders;
      this.tests = tests;
   }

   public void visualizeMeasurements() {
      String longestPrefix = RCAVisualizer.getLongestPrefix(tests);
      LOG.debug("Prefix: {} Keys: {}", longestPrefix, tests);

      File detailResultsFolder = new File(dataFolder, "measurements");

      File[] files = dataFolder.listFiles((FileFilter) new WildcardFileFilter("*.json"));
      LOG.debug("Searching in {} Files: {}", dataFolder, files != null ? files.length : "no files");
      readFiles(longestPrefix, detailResultsFolder, files);
   }

   private void readFiles(String longestPrefix, File detailResultsFolder, File[] files) {
      if (files != null) {
         Arrays.sort(files);
         for (File testcaseFile : files) {
            try {
               Kopemedata data = JSONDataLoader.loadData(testcaseFile);

               TestCase testcase = new TestCase(data);

               KoPeMeTreeConverter treeConverter = new KoPeMeTreeConverter(detailResultsFolder, version, testcase);
               File testcaseVisualizationFile = generateJSFile(testcase, treeConverter);

               LOG.debug("Adding action: " + testcase.toString());

               String name = testcase.toString().replace("#", "_").substring(longestPrefix.length());

               final String content = FileUtils.readFileToString(testcaseVisualizationFile, StandardCharsets.UTF_8);
               run.addAction(new MeasurementVisualizationAction("measurement_" + name, content));
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   public Map<String, TestcaseStatistic> getNoWarmupStatistics() {
      return noWarmupStatistics;
   }

   private File generateJSFile(final TestCase testcase, final KoPeMeTreeConverter treeConverter) throws IOException {
      GraphNode kopemeDataNode = treeConverter.getData();

      LOG.info("Statistic: {}", kopemeDataNode.getStatistic());
      noWarmupStatistics.put(testcase.toString(), kopemeDataNode.getStatistic());

      File versionVisualizationFolder = new File(visualizationFolders.getVisualizationFolder(), version);
      File kopemeVisualizationFolder = new File(versionVisualizationFolder, "pure_kopeme");
      if (!kopemeVisualizationFolder.mkdirs()) {
         LOG.error("Creating file {} was not possibley", kopemeVisualizationFolder);
      }
      File testcaseVisualizationFile = new File(kopemeVisualizationFolder, testcase.getClazz() + "_" + testcase.getMethodWithParams() + ".json");
      writeDataJS(testcaseVisualizationFile, kopemeDataNode);
      return testcaseVisualizationFile;
   }

   private void writeDataJS(final File destFile, final GraphNode kopemeDataNode) throws IOException {
      try (final BufferedWriter fileWriter = Files.newWriter(destFile, StandardCharsets.UTF_8)) {
         fileWriter.write("var treeData = {};\n\n");
         fileWriter.write("var kopemeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(kopemeDataNode));
         fileWriter.write("];\n");
      }

   }
}
