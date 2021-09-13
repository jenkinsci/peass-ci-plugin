package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;
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

         System.out.println("Deleting " + resultsFolders.getChangeFile());
         resultsFolders.getChangeFile().delete();
         System.out.println("Deleting " + resultsFolders.getStatisticsFile());
         resultsFolders.getStatisticsFile().delete();
         System.out.println("Deleting " + resultsFolders.getRtsLogFolder());
         resultsFolders.getRtsLogFolder().delete();
         System.out.println("Deleting " + resultsFolders.getMeasurementLogFolder());
         FileUtils.deleteDirectory(resultsFolders.getMeasurementLogFolder());
         System.out.println("Deleting " + resultsFolders.getRCALogFolder());
         FileUtils.deleteDirectory(resultsFolders.getRCALogFolder());

         PeassFolders folders = new PeassFolders(new File(folder, projectName));
         FileUtils.deleteDirectory(folders.getProjectFolder());
         if (folders.getPeassFolder().exists()) {
            System.out.println("Deleting " + folders.getPeassFolder().getAbsolutePath());
            FileUtils.deleteDirectory(folders.getPeassFolder());
         }

         for (File oldMeasurementFolder : folder.listFiles((FileFilter) new WildcardFileFilter(ResultsFolders.MEASUREMENT_PREFIX + "*"))) {
            System.out.println("Deleting: " + oldMeasurementFolder);
            FileUtils.deleteDirectory(oldMeasurementFolder);
         }

         return true;
      } catch (IOException e) {
         e.printStackTrace();
         return false;
      }
   }
}
