package de.dagere.peass.ci.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.peass.ci.MeasurementVisualizationAction;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
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

   public DefaultMeasurementVisualizer(final File dataFolder, final String version, final Run<?, ?> run, final VisualizationFolderManager visualizationFolders, final Set<String> tests) {
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
      
      LOG.debug("Searching in {} Files: {}", dataFolder, dataFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml")).length);
      for (File testcaseFile : dataFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
         try {
            Kopemedata data = XMLDataLoader.loadData(testcaseFile);
            
            TestCase testcase = new TestCase(data.getTestcases(), "");
            
            KoPeMeTreeConverter treeConverter = new KoPeMeTreeConverter(detailResultsFolder, version, testcase);
            File testcaseVisualizationFile = generateJSFile(testcase, treeConverter);
            
            LOG.debug("Adding action: " + testcase.getExecutable());
            
            String name = testcase.getExecutable().replace("#", "_").substring(longestPrefix.length() + 1);
            
            final String content = FileUtils.readFileToString(testcaseVisualizationFile, StandardCharsets.UTF_8);
            run.addAction(new MeasurementVisualizationAction("measurement_" + name, content));
         } catch (JAXBException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   
   public Map<String, TestcaseStatistic> getNoWarmupStatistics() {
      return noWarmupStatistics;
   }

   private File generateJSFile(final TestCase testcase, final KoPeMeTreeConverter treeConverter) throws IOException {
      GraphNode kopemeDataNode = treeConverter.getData();

      LOG.info("Statistic: {}", kopemeDataNode.getStatistic());
      noWarmupStatistics.put(testcase.getExecutable(), kopemeDataNode.getStatistic());
      
      File versionVisualizationFolder = new File(visualizationFolders.getVisualizationFolder(), version);
      File kopemeVisualizationFolder = new File(versionVisualizationFolder, "pure_kopeme");
      kopemeVisualizationFolder.mkdirs();
      File testcaseVisualizationFile = new File(kopemeVisualizationFolder, testcase.getClazz() + "_" + testcase.getMethod() + ".json");
      writeDataJS(testcaseVisualizationFile, kopemeDataNode);
      return testcaseVisualizationFile;
   }
   
   private void writeDataJS(final File destFile, final GraphNode kopemeDataNode) throws IOException {
      try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(destFile))) {
         fileWriter.write("var treeData = {};\n\n");
         fileWriter.write("var kopemeData = [\n");
         fileWriter.write(Constants.OBJECTMAPPER.writeValueAsString(kopemeDataNode));
         fileWriter.write("];\n");
      }
      
   }
}
