package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousExecutor;
import de.peass.ci.RCAVisualizationAction;
import de.peass.visualization.GraphNode;
import de.peass.visualization.VisualizeRCA;
import hudson.model.Run;

public class RCAVisualizer {
   final ContinuousExecutor executor;
   final ProjectChanges changes;
   final Run<?, ?> run;

   public RCAVisualizer(ContinuousExecutor executor, ProjectChanges changes, Run<?, ?> run) {
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
      System.out.println("Setting property folder: " + executor.getPropertyFolder());
      visualizer.setPropertyFolder(executor.getPropertyFolder());
      visualizer.setResultFolder(resultFolder);
      return visualizer;
   }

   private void createVisualizationActions(File rcaResults, Changes versionChanges, File versionVisualizationFolder) throws IOException {
      String longestPrefix = getLongestPrefix(versionChanges);

      System.out.println("Creating actions: " + versionChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final String name = testcases.getKey() + "_" + change.getMethod();
            File jsFile = new File(versionVisualizationFolder, name + ".js");
            System.out.println("Trying to move " + jsFile.getAbsolutePath());
            if (jsFile.exists()) {
               String destName = testcases.getKey() + "_" + change.getMethod() + ".js";
               File rcaDestFile = new File(rcaResults, destName);
               FileUtils.copyFile(jsFile, rcaDestFile);

               System.out.println("Adding: " + rcaDestFile + " " + name);
               String displayName = name.substring(longestPrefix.length() + 1);
               run.addAction(new RCAVisualizationAction(displayName, rcaDestFile));
            } else {
               System.out.println("An error occured: " + jsFile.getAbsolutePath() + " not found");
            }
         }
      }
   }

   private String getLongestPrefix(Changes versionChanges) {
      String longestPrefix = versionChanges.getTestcaseChanges().keySet().iterator().next();
      for (final String clazz : versionChanges.getTestcaseChanges().keySet()) {
         String withoutClazzItself = clazz.substring(0, clazz.lastIndexOf('.'));
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, withoutClazzItself);
      }
      return longestPrefix;
   }
}