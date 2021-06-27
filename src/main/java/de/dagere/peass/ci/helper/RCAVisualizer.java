package de.dagere.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.visualization.VisualizeRCA;
import de.peass.ci.RCAVisualizationAction;
import hudson.model.Run;

public class RCAVisualizer {

   private static final Logger LOG = LogManager.getLogger(RCAVisualizer.class);

   private final MeasurementConfiguration measurementConfig;
   private final File localWorkspace;
   private final ProjectChanges changes;
   private final Run<?, ?> run;

   public RCAVisualizer(final MeasurementConfiguration measurementConfig, final File localWorkspace, final ProjectChanges changes, final Run<?, ?> run) {
      this.measurementConfig = measurementConfig;
      this.localWorkspace = localWorkspace;
      this.changes = changes;
      this.run = run;
   }

   public void visualizeRCA() throws Exception {
      final File visualizationFolder = getVisualizationFolder();

      VisualizeRCA visualizer = preparePeassVisualizer(visualizationFolder);
      visualizer.call();

      File rcaResults = getRcaResultFolder();

      Changes versionChanges = changes.getVersion(measurementConfig.getVersion());
      File versionVisualizationFolder = new File(visualizationFolder, measurementConfig.getVersion());

      createVisualizationActions(rcaResults, versionChanges, versionVisualizationFolder);
   }

   private File getRcaResultFolder() {
      File rcaResults = new File(run.getRootDir(), "rca_visualization");
      if (!rcaResults.exists()) {
         if (!rcaResults.mkdirs()) {
            throw new RuntimeException("Could not create " + rcaResults.getAbsolutePath());
         }
      }
      return rcaResults;
   }

   private File getVisualizationFolder() {
      final File visualizationFolder = new File(localWorkspace, "visualization");
      if (!visualizationFolder.exists()) {
         if (!visualizationFolder.mkdirs()) {
            throw new RuntimeException("Could not create " + visualizationFolder.getAbsolutePath());
         }
      }
      return visualizationFolder;
   }

   private VisualizeRCA preparePeassVisualizer(final File resultFolder) {
      VisualizeRCA visualizer = new VisualizeRCA();
      File dataFolder = getDataFolder();
      visualizer.setData(new File[] { dataFolder });
      File propertyFolder = new File(localWorkspace, "properties_" + run.getParent().getFullDisplayName());
      if (!propertyFolder.exists()) {
         propertyFolder = new File(localWorkspace, "properties_workspace");
      }
      LOG.info("Setting property folder: " + propertyFolder);
      visualizer.setPropertyFolder(propertyFolder);
      visualizer.setResultFolder(resultFolder);
      return visualizer;
   }

   private File getDataFolder() {
      String rcaResultFolder = run.getParent().getFullDisplayName() + "_peass";
      File dataFolder = new File(localWorkspace, rcaResultFolder);
      if (!dataFolder.exists()) {
         dataFolder = new File(localWorkspace, "workspace_peass");
         if (!dataFolder.exists()) {
            throw new RuntimeException(
                  localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + rcaResultFolder + "; one must exist for visualization!");
         }
      }
      return dataFolder;
   }

   private void createVisualizationActions(final File rcaResults, final Changes versionChanges, final File versionVisualizationFolder) throws IOException {
      String longestPrefix = getLongestPrefix(versionChanges);

      LOG.info("Creating actions: " + versionChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final String name = testcases.getKey() + "_" + change.getMethod();
            File jsFile = new File(versionVisualizationFolder, name + ".js");
            LOG.info("Trying to move " + jsFile.getAbsolutePath());
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

   public static String getLongestPrefix(final Changes versionChanges) {
      String longestPrefix;
      if (versionChanges.getTestcaseChanges().size() > 0) {
         longestPrefix = versionChanges.getTestcaseChanges().keySet().iterator().next();
      } else {
         longestPrefix = "";
      }
      for (final String clazz : versionChanges.getTestcaseChanges().keySet()) {
         String withoutClazzItself = clazz.substring(0, clazz.lastIndexOf('.'));
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, withoutClazzItself);
      }
      return longestPrefix;
   }
}