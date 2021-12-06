package de.dagere.peass.ci.helper;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.model.Run;

public class VisualizationFolderManager {

   private static final Logger LOG = LogManager.getLogger(VisualizationFolderManager.class);

   private final File localWorkspace;
   private final String projectName;
   private final Run<?, ?> run;

   public VisualizationFolderManager(final File localWorkspace, final String projectName, final Run<?, ?> run) {
      this.localWorkspace = localWorkspace;
      this.run = run;
      this.projectName = projectName;
      System.out.println("Workspace name: " + projectName);
   }

   public File getPropertyFolder() {
      File propertyFolder = new File(localWorkspace, "properties_" + projectName);
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
      String rcaResultFolder = projectName + "_peass";
      File dataFolder = new File(localWorkspace, rcaResultFolder);
      if (!dataFolder.exists()) {
         dataFolder = new File(localWorkspace, "workspace_peass");
         if (!dataFolder.exists()) {
            debugListFiles();
            throw new RuntimeException(
                  localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + rcaResultFolder + "; one must exist for visualization!");
         }
      }
      return dataFolder;
   }

   public PeassFolders getPeassFolders() {
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            debugListFiles();
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new PeassFolders(projectFolder, "workspace");
         }
      } else {
         return new PeassFolders(projectFolder, projectName);
      }
   }

   public CauseSearchFolders getPeassRCAFolders() {
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            debugListFiles();
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new CauseSearchFolders(projectFolder);
         }
      } else {
         return new CauseSearchFolders(projectFolder);
      }
   }

   public ResultsFolders getResultsFolders() {
      File projectFolder = new File(localWorkspace, projectName);
      if (!projectFolder.exists()) {
         projectFolder = new File(localWorkspace, "workspace");
         if (!projectFolder.exists()) {
            debugListFiles();
            throw new RuntimeException(localWorkspace.getAbsolutePath() + " neither contains workspace_peass nor " + projectName + "; one must exist for visualization!");
         } else {
            return new ResultsFolders(localWorkspace, "workspace");
         }
      } else {
         return new ResultsFolders(localWorkspace, projectName);
      }
   }

   private void debugListFiles() {
      File[] files = localWorkspace.listFiles();
      if (files != null) {
         LOG.debug("Files: {}", files.length);
         for (File file : files) {
            LOG.info("Existing file: {}", file.getAbsolutePath());
         }
      } else {
         LOG.error("Local workspace did not contain anything");
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
