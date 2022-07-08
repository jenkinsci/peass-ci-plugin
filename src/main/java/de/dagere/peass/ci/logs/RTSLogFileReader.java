package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.utils.Constants;

public class RTSLogFileReader {
   private static final Logger LOG = LogManager.getLogger(RTSLogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfig measurementConfig;
   private final boolean logsExisting;
   private final boolean versionRunWasSuccess;

   public RTSLogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfig measurementConfig) {
      this.visualizationFolders = visualizationFolders;
      this.measurementConfig = measurementConfig;

      File rtsLogOverviewFile = visualizationFolders.getResultsFolders().getRTSLogFile(measurementConfig.getFixedCommitConfig().getCommit(),
            measurementConfig.getFixedCommitConfig().getCommitOld());
      LOG.info("RTS log overview file: {} Exists: {}", rtsLogOverviewFile, rtsLogOverviewFile.exists());
      logsExisting = rtsLogOverviewFile.exists();

      versionRunWasSuccess = isVersionRunSuccess(visualizationFolders, measurementConfig);
   }

   private boolean isVersionRunSuccess(final VisualizationFolderManager visualizationFolders, final MeasurementConfig measurementConfig) {
      boolean success = false;
      File dependencyFile = visualizationFolders.getResultsFolders().getStaticTestSelectionFile();
      if (dependencyFile.exists()) {
         try {
            StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
            VersionStaticSelection version = dependencies.getVersions().get(measurementConfig.getFixedCommitConfig().getCommit());
            if (version != null) {
               LOG.debug("Version run success: {}", version.isRunning());
               success = version.isRunning();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }

      } else {
         LOG.debug("Dependencyfile {} not found, so run was n o success", dependencyFile);
      }
      return success;
   }

   public boolean isVersionRunWasSuccess() {
      return versionRunWasSuccess;
   }

   public boolean isLogsExisting() {
      return logsExisting;
   }

   public Map<TestCase, RTSLogData> getRtsVmRuns(final String version) {
      File versionFolder = new File(visualizationFolders.getPeassFolders().getDependencyLogFolder(), version);
      File[] allFiles = versionFolder.listFiles();
      Map<File, String> testClazzFolders = new LinkedHashMap<>();

      if (allFiles != null) {
         for (File examinedFolder : allFiles) {
            if (examinedFolder.getName().startsWith("log_")) {
               testClazzFolders.put(examinedFolder, null);
            } else if (examinedFolder.isDirectory()) {
               String module = examinedFolder.getName();
               File[] versionFiles = examinedFolder.listFiles((FilenameFilter) new WildcardFileFilter("log_*"));
               if (versionFiles != null) {
                  for (File testClazzFolder : versionFiles) {
                     testClazzFolders.put(testClazzFolder, module);
                  }
               }
            }
         }
      } else {
         LOG.info("Expected rts version folder {} did not exist", versionFolder);
      }
      RTSLogFileVersionReader RTSLogFileVersionReader = new RTSLogFileVersionReader(visualizationFolders, version);
      return RTSLogFileVersionReader.getClazzLogs(testClazzFolders);

   }

   public String getRTSLog() {
      File rtsLogFile = visualizationFolders.getResultsFolders().getRTSLogFile(measurementConfig.getFixedCommitConfig().getCommit(),
            measurementConfig.getFixedCommitConfig().getCommitOld());
      try {
         LOG.debug("Reading RTS Log {}", rtsLogFile.getAbsolutePath());
         return FileUtils.readFileToString(rtsLogFile, StandardCharsets.UTF_8);
      } catch (IOException e) {
         e.printStackTrace();
         return "RTS log not readable";
      }
   }

   public String getSourceReadingLog() {
      File sourceReadLogFile = visualizationFolders.getResultsFolders().getSourceReadLogFile(measurementConfig.getFixedCommitConfig().getCommit(),
            measurementConfig.getFixedCommitConfig().getCommitOld());
      try {
         if (sourceReadLogFile.exists()) {
            LOG.debug("Reading Source Read Log {}", sourceReadLogFile.getAbsolutePath());
            return FileUtils.readFileToString(sourceReadLogFile, StandardCharsets.UTF_8);
         } else {
            LOG.error("No source reading log available");
            return "No source reading log available";
         }
      } catch (IOException e) {
         e.printStackTrace();
         return "RTS log not readable";
      }
   }

   public Map<String, File> findProcessSuccessRuns() {
      Map<String, File> commitSuccessTestRuns = new LinkedHashMap<>();
      addCommitSuccessRun(commitSuccessTestRuns, measurementConfig.getFixedCommitConfig().getCommit());
      addCommitSuccessRun(commitSuccessTestRuns, measurementConfig.getFixedCommitConfig().getCommitOld());
      return commitSuccessTestRuns;
   }

   private void addCommitSuccessRun(final Map<String, File> commitSuccessTestRuns, final String checkSuccessRunVersion) {
      File candidate = visualizationFolders.getPeassFolders().getDependencyLogSuccessRunFile(checkSuccessRunVersion);
      if (candidate.exists()) {
         LOG.info("RTS process success run {} exists", candidate.getAbsolutePath());
         commitSuccessTestRuns.put(checkSuccessRunVersion, candidate);
      } else {
         LOG.info("RTS process success run {} did not exist", candidate.getAbsolutePath());
      }
   }
}
