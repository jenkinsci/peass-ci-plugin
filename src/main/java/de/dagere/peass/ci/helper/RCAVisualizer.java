package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.IOException;
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
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.visualization.VisualizeRCA;
import hudson.model.Run;

public class RCAVisualizer {

   private static final Logger LOG = LogManager.getLogger(RCAVisualizer.class);

   private final MeasurementConfiguration measurementConfig;
   private final VisualizationFolderManager visualizationFolders;
   private final ProjectChanges changes;
   private final Run<?, ?> run;

   public RCAVisualizer(final MeasurementConfiguration measurementConfig, final VisualizationFolderManager visualizationFolders, final ProjectChanges changes, final Run<?, ?> run) {
      this.measurementConfig = measurementConfig;
      this.visualizationFolders = visualizationFolders;
      this.changes = changes;
      this.run = run;
   }

   public void visualizeRCA() throws Exception {
      final File visualizationFolder = visualizationFolders.getVisualizationFolder();

      VisualizeRCA visualizer = preparePeassVisualizer(visualizationFolder);
      visualizer.call();

      File rcaResults = visualizationFolders.getRcaResultFolder();

      Changes versionChanges = changes.getVersion(measurementConfig.getVersion());
      File versionVisualizationFolder = new File(visualizationFolder, measurementConfig.getVersion());

      createVisualizationActions(rcaResults, versionChanges, versionVisualizationFolder);
   }

   private VisualizeRCA preparePeassVisualizer(final File resultFolder) {
      VisualizeRCA visualizer = new VisualizeRCA();
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
            final String name = testcases.getKey() + "_" + change.getMethod();
            File jsFile = new File(versionVisualizationFolder, name + ".js");
            LOG.info("Trying to copy {} Exists: {}", jsFile.getAbsolutePath(), jsFile.exists());
            if (jsFile.exists()) {
               final String destName = testcases.getKey() + "_" + change.getMethod() + ".js";
               final File rcaDestFile = new File(rcaResults, destName);
               FileUtils.copyFile(jsFile, rcaDestFile);

               LOG.info("Adding: " + rcaDestFile + " " + name);
               final String displayName = name.substring(longestPrefix.length() + 1);
               run.addAction(new RCAVisualizationAction(displayName, rcaDestFile));
            } else {
               LOG.error("An error occured: " + jsFile.getAbsolutePath() + " not found");
            }
         }
      }
   }

   public static String getLongestPrefix(final Set<String> tests) {
      String longestPrefix;
      if (tests.size() > 0) {
         longestPrefix = tests.iterator().next();
      } else {
         longestPrefix = "";
      }
      for (final String clazz : tests) {
         String withoutClazzItself = clazz.substring(0, clazz.lastIndexOf('.'));
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, withoutClazzItself);
      }
      return longestPrefix;
   }
}