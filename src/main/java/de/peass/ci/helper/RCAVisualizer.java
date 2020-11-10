package de.peass.ci.helper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousExecutor;
import de.peass.ci.RCAVisualizationAction;
import de.peass.visualization.VisualizeRCA;
import hudson.model.Run;

public class RCAVisualizer{
   final ContinuousExecutor executor;
   final ProjectChanges changes;
   final Run<?, ?> run;
   
   public RCAVisualizer(ContinuousExecutor executor, ProjectChanges changes, Run<?, ?> run) {
      this.executor = executor;
      this.changes = changes;
      this.run = run;
   }
   
   public void visualizeRCA() throws Exception {
      VisualizeRCA visualizer = new VisualizeRCA();
      visualizer.setData(new File[] { executor.getFolders().getFullMeasurementFolder().getParentFile() });
      System.out.println("Setting property folder: " + executor.getPropertyFolder());
      visualizer.setPropertyFolder(executor.getPropertyFolder());
      final File resultFolder = new File(executor.getLocalFolder(), "visualization");
      resultFolder.mkdirs();
      visualizer.setResultFolder(resultFolder);
      visualizer.call();

      File rcaResults = new File(run.getRootDir(), "rca_visualization");
      rcaResults.mkdirs();

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      File versionVisualizationFolder = new File(resultFolder, executor.getLatestVersion());

      createVisualizationActions(rcaResults, versionChanges, versionVisualizationFolder);
   }
   
   private void createVisualizationActions(File rcaResults, Changes versionChanges, File versionVisualizationFolder) throws IOException {
      System.out.println("Creating actions: " + versionChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final String name = testcases.getKey() + "#" + change.getMethod();
            File htmlFile = new File(versionVisualizationFolder, name + ".html");
            System.out.println("Trying to move " + htmlFile.getAbsolutePath());
            if (htmlFile.exists()) {
               String destName = testcases.getKey() + "_" + change.getMethod() + ".html";
               File rcaDestFile = new File(rcaResults, destName);
               FileUtils.copyFile(htmlFile, rcaDestFile);

               System.out.println("Adding: " + rcaDestFile + " " + name);
               run.addAction(new RCAVisualizationAction(name, rcaDestFile));
            } else {
               System.out.println("An error occured: " + htmlFile.getAbsolutePath() + " not found");
            }
         }
      }
   }
}