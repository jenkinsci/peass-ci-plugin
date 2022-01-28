package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.folders.PeassFolders;

public class CleanUtil {
   public static void cleanProjectFolder(final File folder, final String projectName) throws IOException {
      PeassFolders folders = new PeassFolders(new File(folder, projectName));
      FileUtils.deleteDirectory(folders.getProjectFolder());
      if (folders.getPeassFolder().exists()) {
         System.out.println("Deleting " + folders.getPeassFolder().getAbsolutePath());
         FileUtils.deleteDirectory(folders.getPeassFolder());
      }
   }
}
