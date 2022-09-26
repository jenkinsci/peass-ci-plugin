package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.folders.PeassFolders;

public class CleanUtil {
   public static void cleanProjectFolder(final File folder, final String projectName) throws IOException {
      File projectFolder = new File(folder, projectName);
      if (projectFolder.exists()) {
         try {
            PeassFolders peassFolders = new PeassFolders(projectFolder);
            FileUtils.deleteDirectory(peassFolders.getProjectFolder());
            if (peassFolders.getPeassFolder().exists()) {
               System.out.println("Deleting " + peassFolders.getPeassFolder().getAbsolutePath());
               FileUtils.deleteDirectory(peassFolders.getPeassFolder());
            }
         } catch (RuntimeException e) {
            System.err.println("For some reason, folder was already cleaned partially; consider fully cleaning manually");
            e.printStackTrace();
         }
      } else {
         System.err.println("Project folder " + projectFolder.getAbsolutePath() + " did not exist; did not clean it");
      }

   }
}
