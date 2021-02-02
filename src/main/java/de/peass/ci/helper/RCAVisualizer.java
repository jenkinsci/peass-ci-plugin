package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousExecutor;
import de.peass.ci.RCAVisualizationAction;
import de.peass.visualization.VisualizeRCA;
import hudson.model.Run;

public class RCAVisualizer {
   
   private static final Logger LOG = LogManager.getLogger(RCAVisualizer.class);
   
   private final ContinuousExecutor executor;
   private final ProjectChanges changes;
   private final Run<?, ?> run;

   public RCAVisualizer(final ContinuousExecutor executor, final ProjectChanges changes, final Run<?, ?> run) {
      this.executor = executor;
      this.changes = changes;
      this.run = run;
   }

   public void visualizeRCA() throws Exception {
      final File resultFolder = new File(executor.getLocalFolder(), "visualization");
      resultFolder.mkdirs();

      VisualizeRCA visualizer = preparePeassVisualizer(resultFolder);
      visualizer.call();

      File rcaResults = new File(run.getRootDir(), "rca_visualization");
      rcaResults.mkdirs();

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      File versionVisualizationFolder = new File(resultFolder, executor.getLatestVersion());

      createVisualizationActions(rcaResults, versionChanges, versionVisualizationFolder);
   }

   private VisualizeRCA preparePeassVisualizer(final File resultFolder) {
      VisualizeRCA visualizer = new VisualizeRCA();
      visualizer.setData(new File[] { executor.getFolders().getFullMeasurementFolder().getParentFile() });
      LOG.info("Setting property folder: " + executor.getPropertyFolder());
      visualizer.setPropertyFolder(executor.getPropertyFolder());
      visualizer.setResultFolder(resultFolder);
      return visualizer;
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