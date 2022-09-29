package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class CleanRTSCallable implements FileCallable<Boolean> {

   private static final long serialVersionUID = 6370053198339266977L;
   
   private final TaskListener listener;

   public CleanRTSCallable(final TaskListener listener) {
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {

   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) throws IOException, InterruptedException {
      try (final JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         String projectName = potentialSlaveWorkspace.getName();
         File folder = new File(potentialSlaveWorkspace.getParentFile(), projectName + PeassFolders.PEASS_FULL_POSTFIX);
         ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

         deleteResultFiles(resultsFolders);
         deleteLogFolders(resultsFolders);

         return true;
      } catch (IOException e) {
         listener.getLogger().println("Exception thrown");
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return false;
      }
   }
   
   public static void cleanFolder(final String projectName, final File folder) throws IOException {
      System.out.println("Trying " + folder + " " + projectName);
      ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

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

   }

   private static void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRtsLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRtsLogFolder());
      System.out.println("Deleting " + resultsFolders.getSourceReadLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getSourceReadLogFolder());
   }

}
