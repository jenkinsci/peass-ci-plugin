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
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.traces.TraceWriter;
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

      File rtsLogOverviewFile = visualizationFolders.getResultsFolders().getDependencyLogFile(measurementConfig.getExecutionConfig().getVersion(),
            measurementConfig.getExecutionConfig().getVersionOld());
      LOG.info("RTS log overview file: {} Exists: {}", rtsLogOverviewFile, rtsLogOverviewFile.exists());
      logsExisting = rtsLogOverviewFile.exists();

      versionRunWasSuccess = isVersionRunSuccess(visualizationFolders, measurementConfig);
   }

   private boolean isVersionRunSuccess(final VisualizationFolderManager visualizationFolders, final MeasurementConfig measurementConfig) {
      boolean success;
      File dependencyFile = visualizationFolders.getResultsFolders().getDependencyFile();
      if (dependencyFile.exists()) {
         try {
            Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
            VersionStaticSelection version = dependencies.getVersions().get(measurementConfig.getExecutionConfig().getVersion());
            if (version != null) {
               LOG.debug("Version run success: {}", version.isRunning());
               success = version.isRunning();
            } else {
               success = false;
            }
         } catch (IOException e) {
            success = false;
            e.printStackTrace();
         }

      } else {
         LOG.debug("Dependencyfile {} not found, so run was n o success", dependencyFile);
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
      File[] allFiles = versionFolder.listFiles();
      if (allFiles != null) {
         for (File examinedFolder : allFiles) {
            if (examinedFolder.getName().startsWith("log_")) {
               getClazzLogs(version, null, files, examinedFolder);
            } else if (examinedFolder.isDirectory()) {
               String module = examinedFolder.getName();
               File[] versionFiles = examinedFolder.listFiles((FilenameFilter) new WildcardFileFilter("log_*"));
               if (versionFiles != null) {
                  for (File testClazzFolder : versionFiles) {
                     getClazzLogs(version, module, files, testClazzFolder);
                  }
               }
            }
         }
      } else {
         LOG.info("Expected rts version folder {} did not exist", versionFolder);
      }

      return files;
   }

   private void getClazzLogs(final String version, final String module, final Map<TestCase, RTSLogData> files, final File testClazzFolder) {
      LOG.debug("Looking for method files in {}", testClazzFolder.getAbsolutePath());
      File[] methodFiles = testClazzFolder.listFiles();
      if (methodFiles != null) {
         for (File methodFile : methodFiles) {
            LOG.debug("Looking for method log file in {}", methodFile.getAbsolutePath());
            if (!methodFile.isDirectory()) {
               String clazz = testClazzFolder.getName().substring("log_".length());
               String method = methodFile.getName().substring(0, methodFile.getName().length() - ".txt".length());
               TestCase test = new TestCase(clazz, method, module);

               addMethodLog(version, files, testClazzFolder, methodFile, test);
            }
         }
      }
   }

   private void addMethodLog(final String version, final Map<TestCase, RTSLogData> files, final File testClazzFolder,
         final File methodFile, final TestCase test) {
      File clazzDir = visualizationFolders.getResultsFolders().getClazzDir(version, test);
      File viewMethodDir = new File(clazzDir, test.getMethodWithParams());
      boolean foundAnyParameterized = false;
      if (!viewMethodDir.exists()) {
         foundAnyParameterized = addParameterizedMethodLogs(version, files, testClazzFolder, methodFile, test, clazzDir);
      }
      if (!foundAnyParameterized) {
         addRegularMethodLog(version, files, testClazzFolder, methodFile, test, viewMethodDir);
      }
   }

   private boolean addParameterizedMethodLogs(final String version, final Map<TestCase, RTSLogData> files, final File testClazzFolder, final File methodFile, final TestCase test,
         File clazzDir) {
      boolean foundAnyParameterized = false;
      File[] potentialParameterFiles = clazzDir.listFiles();
      if (potentialParameterFiles != null) {
         for (File potentialParameterizedFile : potentialParameterFiles) {
            String fileName = potentialParameterizedFile.getName();
            if (fileName.startsWith(test.getMethodWithParams() + "(")) {
               foundAnyParameterized = true;
               LOG.debug("Found parameterized trace file: {}", potentialParameterizedFile);

               String params = fileName.substring(test.getMethod().length() + 1, fileName.length() - 1);
               TestCase testWithparams = new TestCase(test.getClazz(), test.getMethod(), test.getModule(), params);
               addMethodLogData(version, files, testClazzFolder, methodFile, testWithparams, true);
            }
         }
      }
      return foundAnyParameterized;
   }

   private void addRegularMethodLog(final String version, final Map<TestCase, RTSLogData> files, final File testClazzFolder, final File methodFile, final TestCase test,
         File viewMethodDir) {
      File viewMethodFile = new File(viewMethodDir, TraceWriter.getShortVersion(version));
      boolean runWasSuccessful = viewMethodFile.exists();
      addMethodLogData(version, files, testClazzFolder, methodFile, test, runWasSuccessful);
   }

   private void addMethodLogData(final String version, final Map<TestCase, RTSLogData> files, final File testClazzFolder, final File methodFile, final TestCase test,
         boolean runWasSuccessful) {
      File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
      RTSLogData data = new RTSLogData(version, methodFile, cleanFile, runWasSuccessful);

      files.put(test, data);
      LOG.debug("Adding log: {}", test);
   }

   public String getRTSLog() {
      File rtsLogFile = visualizationFolders.getResultsFolders().getDependencyLogFile(measurementConfig.getExecutionConfig().getVersion(),
            measurementConfig.getExecutionConfig().getVersionOld());
      try {
         LOG.debug("Reading RTS Log {}", rtsLogFile.getAbsolutePath());
         String rtsLog = FileUtils.readFileToString(rtsLogFile, StandardCharsets.UTF_8);
         return rtsLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "RTS log not readable";
      }
   }

   public String getSourceReadingLog() {
      File sourceReadLogFile = visualizationFolders.getResultsFolders().getSourceReadLogFile(measurementConfig.getExecutionConfig().getVersion(),
            measurementConfig.getExecutionConfig().getVersionOld());
      try {
         LOG.debug("Reading Source Read Log{}", sourceReadLogFile.getAbsolutePath());
         String sourceReadLog = FileUtils.readFileToString(sourceReadLogFile, StandardCharsets.UTF_8);
         return sourceReadLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "RTS log not readable";
      }
   }

   public Map<String, File> findProcessSuccessRuns() {
      Map<String, File> processSuccessTestRuns = new LinkedHashMap<>();
      addVersionRun(processSuccessTestRuns, measurementConfig.getExecutionConfig().getVersion());
      addVersionRun(processSuccessTestRuns, measurementConfig.getExecutionConfig().getVersionOld());
      return processSuccessTestRuns;
   }

   private void addVersionRun(final Map<String, File> processSuccessTestRuns, final String checkSuccessRunVersion) {
      File candidate = visualizationFolders.getPeassFolders().getDependencyLogSuccessRunFile(checkSuccessRunVersion);
      if (candidate.exists()) {
         LOG.info("RTS process success run {} exists", candidate.getAbsolutePath());
         processSuccessTestRuns.put(checkSuccessRunVersion, candidate);
      } else {
         LOG.info("RTS process success run {} did not exist", candidate.getAbsolutePath());
      }
   }
}
