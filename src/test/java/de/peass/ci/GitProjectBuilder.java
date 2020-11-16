package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Builds a git project which can be used for integration testing 
 * @author reichelt
 *
 */
public class GitProjectBuilder {
   
   private final File gitFolder;
   
   public GitProjectBuilder(File destination, File firstVersionFolder) throws InterruptedException, IOException {
      this.gitFolder = destination;
      if (!gitFolder.exists()) {
         gitFolder.mkdirs();
      }
      
      final Process initProcess = Runtime.getRuntime().exec("git init", new String[0], destination);
      initProcess.waitFor();
      
      addVersion(firstVersionFolder, "Initial Commit");
      
      
   }
   
   /**
    * Adds a version to the project, replacing all contents of the repository with the contents of the given folder
    * @param version
    * @throws IOException 
    * @throws InterruptedException 
    */
   public void addVersion(File versionFolder, String commitMessage) throws IOException, InterruptedException {
      FileUtils.copyDirectory(versionFolder, gitFolder);
      
      final Process addProcess = Runtime.getRuntime().exec("git add -A", new String[0], gitFolder);
      addProcess.waitFor();
      
      ProcessBuilder processBuilder = new ProcessBuilder("git", "commit", "-m", commitMessage);
      processBuilder.directory(gitFolder);
      final Process commitProcess = processBuilder.start();
      commitProcess.waitFor();
   }
}
