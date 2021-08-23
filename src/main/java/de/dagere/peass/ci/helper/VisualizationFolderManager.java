package de.dagere.peass.ci.helper;

import java.io.File;

import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;
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
      String projectName = run.getParent().getFullDisplayName();
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new PeassFolders(projectFolder, "workspace");
         }
      } else {
         return new PeassFolders(projectFolder, run.getParent().getFullDisplayName());
      }
   }
   
   public CauseSearchFolders getPeassRCAFolders() {
      String projectName = run.getParent().getFullDisplayName();
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new CauseSearchFolders(projectFolder);
         }
      } else {
         return new CauseSearchFolders(projectFolder);
      }
   }

   public ResultsFolders getResultsFolders() {
      String projectName = run.getParent().getFullDisplayName();
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new ResultsFolders(localWorkspace, "workspace");
         }
      } else {
         return new ResultsFolders(localWorkspace, run.getParent().getFullDisplayName());
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
