package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.folders.PeassFolders;

public class CleanUtil {
   public static void cleanProjectFolder(final File folder, final String projectName) throws IOException {
      File projectFolder = new File(folder, projectName);
      if (projectFolder.exists()) {
         PeassFolders folders = new PeassFolders(projectFolder);
         FileUtils.deleteDirectory(folders.getProjectFolder());
         if (folders.getPeassFolder().exists()) {
            System.out.println("Deleting " + folders.getPeassFolder().getAbsolutePath());
            FileUtils.deleteDirectory(folders.getPeassFolder());
         }
      } else {
         System.err.println("Project folder " + projectFolder.getAbsolutePath() + " did not exist; did not clean it");
      }

   }
}
