package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class CleanMeasurementCallable implements FileCallable<Boolean> {
   private static final long serialVersionUID = 4804971173610549315L;

   private final TaskListener listener;

   public CleanMeasurementCallable(final TaskListener listener) {
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) {
      try (final JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         String projectName = potentialSlaveWorkspace.getName();
         File folder = new File(potentialSlaveWorkspace.getParentFile(), projectName + PeassFolders.PEASS_FULL_POSTFIX);
         ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

         deleteAllMeasurementData(projectName, folder, resultsFolders);

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
      
      File trendFile = new File(folder, TrendFileUtil.TREND_FILE_NAME);
      if (trendFile.exists()) {
         System.out.println("Deleting " + trendFile);
         System.out.println("Success: " + trendFile.delete());
      }

      deleteAllMeasurementData(projectName, folder, resultsFolders);
   }

   private static void deleteAllMeasurementData(final String projectName, final File folder, final ResultsFolders resultsFolders) throws IOException {
      deleteResultFiles(resultsFolders);
      deleteLogFolders(resultsFolders);

      deleteCopiedFolders(folder);
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
