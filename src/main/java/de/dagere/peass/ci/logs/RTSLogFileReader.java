package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.utils.Constants;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class RTSLogFileReader {
   private static final Logger LOG = LogManager.getLogger(RTSLogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfiguration measurementConfig;
   private final boolean logsExisting;
   private final boolean versionRunWasSuccess;

   public RTSLogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfiguration measurementConfig) {
      this.visualizationFolders = visualizationFolders;
      this.measurementConfig = measurementConfig;

      File rtsLogOverviewFile = visualizationFolders.getResultsFolders().getDependencyLogFile(measurementConfig.getVersion(), measurementConfig.getVersionOld());
      LOG.info("RTS log overview file: {} Exists: {}", rtsLogOverviewFile, rtsLogOverviewFile.exists());
      logsExisting = rtsLogOverviewFile.exists();

      versionRunWasSuccess = isVersionRunSuccess(visualizationFolders, measurementConfig);
   }

   private boolean isVersionRunSuccess(final VisualizationFolderManager visualizationFolders, final MeasurementConfiguration measurementConfig) {
      boolean success;
      File dependencyFile = visualizationFolders.getResultsFolders().getDependencyFile();
      if (dependencyFile.exists()) {
         try {
            Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
            Version version = dependencies.getVersions().get(measurementConfig.getVersion());
            if (version != null) {
               success = version.isRunning();
            } else {
               success = false;
            }
         } catch (IOException e) {
            success = false;
            e.printStackTrace();
         }

      } else {
         success = false;
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
      Map<TestCase, RTSLogData> files = new LinkedHashMap<>();
      File versionFolder = new File(visualizationFolders.getPeassFolders().getDependencyLogFolder(), version);
      if (versionFolder.exists()) {
         for (File testClazzFolder : versionFolder.listFiles((FileFilter) new WildcardFileFilter("log_*"))) {
            LOG.debug("Looking for method files in {}", testClazzFolder.getAbsolutePath());
            for (File methodFile : testClazzFolder.listFiles()) {
               LOG.debug("Looking for method log file in {}", methodFile.getAbsolutePath());
               if (!methodFile.isDirectory()) {
                  addMethodLog(version, files, testClazzFolder, methodFile);
               }
            }
         }
      } else {
         LOG.info("Expected rts version folder {} did not exist", versionFolder);
      }
      return files;
   }

   private void addMethodLog(final String version, final Map<TestCase, RTSLogData> files, final File testClazzFolder, final File methodFile) {
      File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
      RTSLogData data = new RTSLogData(version, methodFile, cleanFile);
      String clazz = testClazzFolder.getName().substring("log_".length());
      String method = methodFile.getName().substring(0, methodFile.getName().length() - ".txt".length());
      TestCase test = new TestCase(clazz + "#" + method);
      files.put(test, data);
      LOG.debug("Adding log: {}", test);
   }

   public String getRTSLog() {
      File rtsLogFile = visualizationFolders.getResultsFolders().getDependencyLogFile(measurementConfig.getVersion(), measurementConfig.getVersionOld());
      try {
         LOG.debug("Reading {}", rtsLogFile.getAbsolutePath());
         String rtsLog = FileUtils.readFileToString(rtsLogFile, StandardCharsets.UTF_8);
         return rtsLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "RTS log not readable";
      }
   }

   public Map<String, File> findProcessSuccessRuns() {
      Map<String, File> processSuccessTestRuns = new LinkedHashMap<>();
      addVersionRun(processSuccessTestRuns, measurementConfig.getVersion());
      addVersionRun(processSuccessTestRuns, measurementConfig.getVersionOld());
      return processSuccessTestRuns;
   }

   private void addVersionRun(final Map<String, File> processSuccessTestRuns, final String checkSuccessRunVersion) {
      File candidate = visualizationFolders.getPeassFolders().getDependencyLogSuccessRunFile(checkSuccessRunVersion);
      if (candidate.exists()) {
         processSuccessTestRuns.put(checkSuccessRunVersion, candidate);
      } else {
         LOG.info("RTS version result {} did not exist", candidate);
      }
   }
}
