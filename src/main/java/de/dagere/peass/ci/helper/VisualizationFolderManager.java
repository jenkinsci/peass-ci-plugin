package de.dagere.peass.ci.helper;

import java.io.File;

import de.dagere.peass.dependency.PeassFolders;
import hudson.model.Run;

public class VisualizationFolderManager {
   private File localWorkspace;
   private final Run<?, ?> run;

   public VisualizationFolderManager(final File localWorkspace, final Run<?, ?> run) {
      this.localWorkspace = localWorkspace;
      this.run = run;
   }

   public File getPropertyFolder() {
      File propertyFolder = new File(localWorkspace, "properties_" + run.getParent().getFullDisplayName());
      if (!propertyFolder.exists()) {
         propertyFolder = new File(localWorkspace, "properties_workspace");
      }
      return propertyFolder;
   }

   public File getRcaResultFolder() {
      File rcaResults = new File(run.getRootDir(), "rca_visualization");
      if (!rcaResults.exists()) {
         if (!rcaResults.mkdirs()) {
            throw new RuntimeException("Could not create " + rcaResults.getAbsolutePath());
         }
      }
      return rcaResults;
   }

   public File getDataFolder() {
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

   public PeassFolders getPeassFolders() {
      String peassResultFolder = run.getParent().getFullDisplayName();
      File dataFolder = new File(localWorkspace, peassResultFolder);
      if (!dataFolder.exists()) {
         dataFolder = new File(localWorkspace, "workspace");
         if (!dataFolder.exists()) {
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + peassResultFolder + "; one must exist for visualization!");
         } else {
            return new PeassFolders(dataFolder, "workspace");
         }
      } else {
         return new PeassFolders(dataFolder, run.getParent().getFullDisplayName());
      }
   }

   public File getVisualizationFolder() {
      final File visualizationFolder = new File(localWorkspace, "visualization");
      if (!visualizationFolder.exists()) {
         if (!visualizationFolder.mkdirs()) {
            throw new RuntimeException("Could not create " + visualizationFolder.getAbsolutePath());
         }
      }
      return visualizationFolder;
   }
}
