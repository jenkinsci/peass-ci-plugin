package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class CleanMeasurementCallable implements FileCallable<Boolean> {
   private static final long serialVersionUID = 4804971173610549315L;

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) {
      try {
         String projectName = potentialSlaveWorkspace.getName();
         File folder = new File(potentialSlaveWorkspace.getParentFile(), projectName + "_fullPeass");
         ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

         deleteResultFiles(resultsFolders);
         deleteLogFolders(resultsFolders);

         PeassFolders folders = new PeassFolders(new File(folder, projectName));
         FileUtils.deleteDirectory(folders.getProjectFolder());
         if (folders.getPeassFolder().exists()) {
            System.out.println("Deleting " + folders.getPeassFolder().getAbsolutePath());
            FileUtils.deleteDirectory(folders.getPeassFolder());
         }

         File[] measurementFolders = folder.listFiles((FileFilter) new WildcardFileFilter(ResultsFolders.MEASUREMENT_PREFIX + "*"));
         if (measurementFolders != null) {
            for (File oldMeasurementFolder : measurementFolders) {
               System.out.println("Deleting: " + oldMeasurementFolder);
               FileUtils.deleteDirectory(oldMeasurementFolder);
            }
         }

         return true;
      } catch (IOException e) {
         e.printStackTrace();
         return false;
      }
   }

   private void deleteResultFiles(final ResultsFolders resultsFolders) {
      System.out.println("Deleting " + resultsFolders.getChangeFile());
      if (!resultsFolders.getChangeFile().delete()) {
         System.err.println("Deletion not successfull");
      }
      System.out.println("Deleting " + resultsFolders.getStatisticsFile());
      if (!resultsFolders.getStatisticsFile().delete()) {
         System.err.println("Deletion not successfull");
      }
   }

   private void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRtsLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRtsLogFolder());
      System.out.println("Deleting " + resultsFolders.getMeasurementLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getMeasurementLogFolder());
      System.out.println("Deleting " + resultsFolders.getRCALogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRCALogFolder());
   }
}
