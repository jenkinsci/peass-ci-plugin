package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.ContinuousFolderUtil;
import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.vcs.VersionControlSystem;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class CleanMeasurementCallable extends CleanCallable {

   private static final long serialVersionUID = 4804971173610549315L;

   public CleanMeasurementCallable(final TaskListener listener) {
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

   public void cleanFolder(final ResultsFolders resultsFolders) throws IOException {
      deleteResultFiles(resultsFolders);
      deleteLogFolders(resultsFolders);

      deleteCopiedFolders(resultsFolders.getResultFolder());
      deleteTrendFile(resultsFolders);
   }

   private static void deleteTrendFile(final ResultsFolders resultsFolders) {
      final File trendFile = new File(resultsFolders.getResultFolder(), TrendFileUtil.TREND_FILE_NAME);
      if (trendFile.exists()) {
         System.out.println("Deleting " + trendFile);
         System.out.println("Success: " + trendFile.delete());
      }
   }

   private static void deleteCopiedFolders(final File folder) throws IOException {
      File[] measurementFolders = folder.listFiles((FileFilter) new WildcardFileFilter(ResultsFolders.MEASUREMENT_PREFIX + "*"));
      if (measurementFolders != null) {
         for (File oldMeasurementFolder : measurementFolders) {
            System.out.println("Deleting: " + oldMeasurementFolder);
            FileUtils.deleteDirectory(oldMeasurementFolder);
         }
      }
   }

   private static void deleteResultFiles(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getChangeFile());
      System.out.println("Success: " + resultsFolders.getChangeFile().delete());
      System.out.println("Deleting " + resultsFolders.getStatisticsFile());
      System.out.println("Success: " + resultsFolders.getStatisticsFile().delete());
   }

   private static void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getMeasurementLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getMeasurementLogFolder());
      CauseSearchFolders peassFolders = resultsFolders.getPeassFolders();
      if (peassFolders != null) {
         System.out.println("Deleting: " + peassFolders.getLogFolders().getMeasureLogFolder());
         FileUtils.deleteDirectory(peassFolders.getLogFolders().getMeasureLogFolder());
      }
   }
}
