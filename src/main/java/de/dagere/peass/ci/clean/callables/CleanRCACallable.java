package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class CleanRCACallable implements FileCallable<Boolean> {

   private static final long serialVersionUID = 2008970638274618905L;

   private final TaskListener listener;

   public CleanRCACallable(final TaskListener listener) {
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

         cleanFolder(resultsFolders);

         return true;
      } catch (IOException e) {
         listener.getLogger().println("Exception thrown");
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return false;
      }
   }

   public static void cleanFolder(final ResultsFolders resultsFolders) throws IOException {

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

      CauseSearchFolders causeSearchFolders = resultsFolders.getPeassFolders();
      if (causeSearchFolders != null) {
         if (causeSearchFolders.getRcaFolder().exists()) {
            System.out.println("Deleting: " + causeSearchFolders.getRcaFolder());
            FileUtils.cleanDirectory(causeSearchFolders.getRcaFolder());
         }

         System.out.println("Deleting: " + causeSearchFolders.getRCALogFolder());
         FileUtils.cleanDirectory(causeSearchFolders.getRCALogFolder());
      } else {
         System.err.println("Project folder " + resultsFolders.getPeassFolders()+ " was not existing - not cleaning");
      }

   }

}
