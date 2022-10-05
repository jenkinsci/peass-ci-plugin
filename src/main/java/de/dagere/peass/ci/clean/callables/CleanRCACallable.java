package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.model.TaskListener;

public class CleanRCACallable extends CleanCallable {

   private static final long serialVersionUID = 2008970638274618905L;

   public CleanRCACallable(final TaskListener listener) {
      super(listener);
   }

   @Override
   public void cleanFolder(final ResultsFolders resultsFolders) throws IOException {

      deleteLogFolders(resultsFolders);

      deleteVisualizationFolder(resultsFolders);

   }

   private static void deleteVisualizationFolder(final ResultsFolders resultsFolders) throws IOException {
      File visualizationFolder = new File(resultsFolders.getResultFolder(), VisualizationFolderManager.VISUALIZATION_FOLDER_NAME);
      if (visualizationFolder.exists()) {
         System.out.println("Deleting " + visualizationFolder);
         FileUtils.deleteDirectory(visualizationFolder);
      }
   }

   private static void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRCALogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRCALogFolder());

      cleanCauseSearchFolders(resultsFolders);

   }

   private static void cleanCauseSearchFolders(final ResultsFolders resultsFolders) throws IOException {
      CauseSearchFolders causeSearchFolders = resultsFolders.getPeassFolders();
      if (causeSearchFolders != null) {
         if (causeSearchFolders.getRcaFolder().exists()) {
            System.out.println("Cleaning: " + causeSearchFolders.getRcaFolder());
            FileUtils.cleanDirectory(causeSearchFolders.getRcaFolder());
         }

         System.out.println("Cleaning: " + causeSearchFolders.getRCALogFolder());
         FileUtils.cleanDirectory(causeSearchFolders.getRCALogFolder());
      } else {
         System.err.println("Project folder " + resultsFolders.getPeassFolders()+ " was not existing - not cleaning");
      }
   }

}
