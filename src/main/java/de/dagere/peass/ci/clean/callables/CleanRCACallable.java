package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class CleanRCACallable implements FileCallable<Boolean> {

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {

   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) throws IOException, InterruptedException {
      try {
         String projectName = potentialSlaveWorkspace.getName();
         File folder = new File(potentialSlaveWorkspace.getParentFile(), projectName + "_fullPeass");
         ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

         deleteRCALogFolder(resultsFolders);
         
         deleteCopiedFolders(folder);
         
         return true;
      } catch (IOException e) {
         e.printStackTrace();
         return false;
      }
   }


   private void deleteCopiedFolders(final File folder) throws IOException {
      File[] measurementFolders = folder.listFiles((FileFilter) new WildcardFileFilter(ResultsFolders.RCA_PREFIX + "*"));
      if (measurementFolders != null) {
         for (File oldMeasurementFolder : measurementFolders) {
            System.out.println("Deleting: " + oldMeasurementFolder);
            FileUtils.deleteDirectory(oldMeasurementFolder);
         }
      }
   }

   public void deleteRCALogFolder(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRCALogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRCALogFolder());
   }

}
