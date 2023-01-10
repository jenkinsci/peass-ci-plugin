package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.ContinuousFolderUtil;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.VersionControlSystem;
import groovyjarjarantlr4.v4.codegen.model.ThrowEarlyExitException;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class CleanRTSCallable extends CleanCallable {

   private static final long serialVersionUID = 6370053198339266977L;
   
   public CleanRTSCallable(TaskListener listener) {
      super(listener);
   }
   
   @Override
   public Boolean invoke(File workspaceFolder, VirtualChannel channel) throws IOException, InterruptedException {
      try {
         File vcsFolder = VersionControlSystem.findVCSFolder(workspaceFolder);
         if (vcsFolder != null) {
            File localFolder = ContinuousFolderUtil.getLocalFolder(vcsFolder);
            String projectName = ContinuousFolderUtil.getSubFolderPath(workspaceFolder);
            ResultsFolders resultsFolders = new ResultsFolders(localFolder, projectName);
            cleanFolder(resultsFolders);
            return true;
         } else {
            System.err.println("Did not find a repository in " + workspaceFolder + " - not cleaning this folder");
            return false;
         }
         
      } catch (Throwable t) {
         t.printStackTrace();
         return false;
      }
   }

   public void cleanFolder(final ResultsFolders resultsFolders) throws IOException{
      deleteResultFiles(resultsFolders);
      deleteLogFolders(resultsFolders);
   }

   private static void deleteResultFiles(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getStaticTestSelectionFile());
      System.out.println("Success: " + resultsFolders.getStaticTestSelectionFile().delete());

      System.out.println("Deleting " + resultsFolders.getTraceTestSelectionFile());
      System.out.println("Success: " + resultsFolders.getTraceTestSelectionFile().delete());
      
      System.out.println("Deleting " + resultsFolders.getCoverageSelectionFile());
      System.out.println("Success: " + resultsFolders.getCoverageSelectionFile().delete());
      
      System.out.println("Deleting " + resultsFolders.getCoverageInfoFile());
      System.out.println("Success: " + resultsFolders.getCoverageInfoFile().delete());

      System.out.println("Deleting " + resultsFolders.getViewFolder());
      FileUtils.deleteDirectory(resultsFolders.getViewFolder());
      System.out.println("Deleting " + resultsFolders.getPropertiesFolder());
      FileUtils.deleteDirectory(resultsFolders.getPropertiesFolder());
      
      CauseSearchFolders peassFolders = resultsFolders.getPeassFolders();
      if (peassFolders != null) {
         System.out.println("Deleting: " + peassFolders.getLogFolders().getDependencyLogFolder());
         FileUtils.deleteDirectory(peassFolders.getLogFolders().getDependencyLogFolder());
      }
   }

   private static void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRtsLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRtsLogFolder());
      System.out.println("Deleting " + resultsFolders.getSourceReadLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getSourceReadLogFolder());
   }

}
