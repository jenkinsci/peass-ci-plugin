package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.visualization.VisualizeRCAStarter;
import hudson.model.Run;

public class RCAVisualizer {

   private static final Logger LOG = LogManager.getLogger(RCAVisualizer.class);

   private final MeasurementConfig measurementConfig;
   private final VisualizationFolderManager visualizationFolders;
   private final ProjectChanges changes;
   private final Run<?, ?> run;

   public RCAVisualizer(final MeasurementConfig measurementConfig, final VisualizationFolderManager visualizationFolders, final ProjectChanges changes, final Run<?, ?> run) {
      this.measurementConfig = measurementConfig;
      this.visualizationFolders = visualizationFolders;
      this.changes = changes;
      this.run = run;
   }

   public void visualizeRCA() throws Exception {
      final File visualizationFolder = visualizationFolders.getVisualizationFolder();

      VisualizeRCAStarter visualizer = preparePeassVisualizer(visualizationFolder);
      visualizer.call();

      File rcaResults = visualizationFolders.getRcaResultFolder();

      Changes versionChanges = changes.getVersion(measurementConfig.getExecutionConfig().getVersion());
      File versionVisualizationFolder = new File(visualizationFolder, measurementConfig.getExecutionConfig().getVersion());

      createVisualizationActions(rcaResults, versionChanges, versionVisualizationFolder);
   }

   private VisualizeRCAStarter preparePeassVisualizer(final File resultFolder) {
      VisualizeRCAStarter visualizer = new VisualizeRCAStarter();
      File dataFolder = visualizationFolders.getDataFolder();
      visualizer.setData(new File[] { dataFolder });
      File propertyFolder = visualizationFolders.getPropertyFolder();
      LOG.info("Setting property folder: " + propertyFolder);
      visualizer.setPropertyFolder(propertyFolder);
      visualizer.setResultFolder(resultFolder);
      return visualizer;
   }

   private void createVisualizationActions(final File rcaResults, final Changes versionChanges, final File versionVisualizationFolder) throws IOException {
      String longestPrefix = getLongestPrefix(versionChanges.getTestcaseChanges().keySet());

      LOG.info("Creating actions: " + versionChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final String actionName;
            if (change.getParams() != null) {
               actionName = testcases.getKey() + "_" + change.getMethod() + "(" + change.getParams() + ")";
            } else {
               actionName = testcases.getKey() + "_" + change.getMethod();
            }
            File jsFile = new File(versionVisualizationFolder, actionName + ".js");
            LOG.info("Trying to copy {} Exists: {}", jsFile.getAbsolutePath(), jsFile.exists());
            if (jsFile.exists()) {
               createRCAAction(rcaResults, longestPrefix, testcases, change, actionName, jsFile);
            } else {
               LOG.error("An error occured: " + jsFile.getAbsolutePath() + " not found");
            }
         }
      }
   }

   public void createRCAAction(final File rcaResults, final String longestPrefix, final Entry<String, List<Change>> testcases, final Change change, final String name,
         final File jsFile)
         throws IOException {
      final String destName = testcases.getKey() + "_" + change.getMethod() + ".js";
      final File rcaDestFile = new File(rcaResults, destName);
      FileUtils.copyFile(jsFile, rcaDestFile);

      LOG.info("Adding: " + rcaDestFile + " " + name);
      final String displayName = name.substring(longestPrefix.length());

      final String content = FileUtils.readFileToString(rcaDestFile, StandardCharsets.UTF_8);
      run.addAction(new RCAVisualizationAction(displayName, content));
   }

   public static String getLongestPrefix(final Set<String> tests) {
      String longestPrefix;
      if (tests.size() > 0) {
         longestPrefix = tests.iterator().next();
      } else {
         longestPrefix = "";
      }
      for (final String clazz : tests) {
         String withoutClazzItself = clazz.substring(0, clazz.lastIndexOf('.') + 1);
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, withoutClazzItself);
      }
      return longestPrefix;
   }
}