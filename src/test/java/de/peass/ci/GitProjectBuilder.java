package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.utils.StreamGobbler;

/**
 * Builds a git project which can be used for integration testing 
 * @author reichelt
 *
 */
public class GitProjectBuilder {
   
   private static final Logger LOG = LogManager.getLogger(GitProjectBuilder.class);
   
   private final File gitFolder;
   
   public GitProjectBuilder(File destination, File firstVersionFolder) throws InterruptedException, IOException {
      this.gitFolder = destination;
      if (!gitFolder.exists()) {
         gitFolder.mkdirs();
      }
      
      final Process initProcess = Runtime.getRuntime().exec("git init", new String[0], destination);
      String initOutput = StreamGobbler.getFullProcess(initProcess, false);
      LOG.debug("Init output: {}", initOutput);
      
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
      String addOutput = StreamGobbler.getFullProcess(addProcess, false);
      LOG.debug("Add output: {}", addOutput);
      
      ProcessBuilder processBuilder = new ProcessBuilder("git", "commit", "-m", commitMessage);
      processBuilder.directory(gitFolder);
      final Process commitProcess = processBuilder.start();
      String commitOutput = StreamGobbler.getFullProcess(commitProcess, false);
      LOG.debug("Commit output: {}", commitOutput);
   }
}
