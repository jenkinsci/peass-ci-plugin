package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.folders.ResultsFolders;
import hudson.model.TaskListener;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class CleanMeasurementCallable extends CleanCallable {

   private static final long serialVersionUID = 4804971173610549315L;

   public CleanMeasurementCallable(final TaskListener listener) {
      super(listener);
   }

   @Override
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
   }
}
