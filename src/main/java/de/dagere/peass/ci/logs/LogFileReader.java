package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfiguration measurementConfig;
   private boolean logsExisting = false;

   public LogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfiguration measurementConfig) {
      this.visualizationFolders = visualizationFolders;
      this.measurementConfig = measurementConfig;
      
      File rtsLogOverviewFile = visualizationFolders.getResultsFolders().getDependencyLogFile(measurementConfig.getVersion(), measurementConfig.getVersionOld());
      logsExisting = rtsLogOverviewFile.exists();
   }
   
   public boolean isLogsExisting() {
      return logsExisting;
   }

   public Map<String, File> findProcessSuccessRuns() {
      Map<String, File> processSuccessTestRuns = new LinkedHashMap<>();
      addVersionRun(processSuccessTestRuns, measurementConfig.getVersion());
      addVersionRun(processSuccessTestRuns, measurementConfig.getVersionOld());
      return processSuccessTestRuns;
   }

   private void addVersionRun(final Map<String, File> processSuccessTestRuns, final String checkSuccessRunVersion) {
      File candidate = new File(visualizationFolders.getPeassFolders().getDependencyLogFolder(), checkSuccessRunVersion + File.separator + "testRunning.log");
      if (candidate.exists()) {
         logsExisting = true;
         processSuccessTestRuns.put(checkSuccessRunVersion, candidate);
      }
   }

   public Map<TestCase, RTSLogData> getRtsVmRuns(final String version) {
      Map<TestCase, RTSLogData> files = new LinkedHashMap<>();
      File versionFolder = new File(visualizationFolders.getPeassFolders().getDependencyLogFolder(), version);
      if (versionFolder.exists()) {
         logsExisting = true;
         for (File testClazzFolder : versionFolder.listFiles((FileFilter) new WildcardFileFilter("log_*"))) {
            for (File methodFile : testClazzFolder.listFiles()) {
               if (!methodFile.isDirectory()) {
                  File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
                  RTSLogData data = new RTSLogData(version, methodFile, cleanFile);
                  String clazz = testClazzFolder.getName().substring("log_".length());
                  String method = methodFile.getName().substring(0, methodFile.getName().length() - ".txt".length());
                  TestCase test = new TestCase(clazz + "#" + method);
                  files.put(test, data);
               }
            }
         }
      }
      return files;
   }

   public Map<TestCase, List<LogFiles>> readAllTestcases(final ProjectStatistics statistics) {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      for (TestCase testcase : statistics.getStatistics().get(measurementConfig.getVersion()).keySet()) {
         readTestcase(visualizationFolders.getPeassFolders(), logFiles, testcase);
      }
      return logFiles;
   }

   public Map<TestCase, List<LogFiles>> readAllRCATestcases() {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      return logFiles;
   }

   private void readTestcase(final PeassFolders folders, final Map<TestCase, List<LogFiles>> logFiles, final TestCase testcase) {
      LOG.info("Reading testcase " + testcase);
      List<LogFiles> currentFiles = new LinkedList<>();
      File logFolder = folders.getExistingMeasureLogFolder(measurementConfig.getVersion(), testcase);
      tryLocalLogFolderVMIds(testcase, currentFiles, logFolder, folders);
      logFiles.put(testcase, currentFiles);
   }

   private void tryLocalLogFolderVMIds(final TestCase testcase, final List<LogFiles> currentFiles, final File logFolder, final PeassFolders folders) {
      LOG.debug("Log folder: {} {}", logFolder, logFolder.listFiles());
      int tryIndex = 0;
      String filenameSuffix = "log_" + testcase.getClazz() + File.separator + testcase.getMethod() + ".txt";
      File predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersionOld() + File.separator + filenameSuffix);
      LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
      while (predecessorFile.exists()) {
         CorrectRunChecker checker = new CorrectRunChecker(testcase, tryIndex, measurementConfig, visualizationFolders);
         
         File currentFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersion() + File.separator + filenameSuffix);
         LogFiles vmidLogFile = new LogFiles(predecessorFile, currentFile, checker.isPredecessorRunning(), checker.isCurrentRunning());
         currentFiles.add(vmidLogFile);

         tryIndex++;
         predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersionOld() + File.separator + filenameSuffix);
         LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
      }
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

   public String getMeasureLog() {
      File measureLogFile = visualizationFolders.getResultsFolders().getMeasurementLogFile(measurementConfig.getVersion(), measurementConfig.getVersionOld());
      try {
         LOG.debug("Reading {}", measureLogFile.getAbsolutePath());
         String rtsLog = FileUtils.readFileToString(measureLogFile, StandardCharsets.UTF_8);
         return rtsLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }

   public String getRCALog() {
      File rcaLogFile = visualizationFolders.getResultsFolders().getRCALogFile(measurementConfig.getVersion(), measurementConfig.getVersionOld());
      try {
         LOG.debug("Reading {}", rcaLogFile.getAbsolutePath());
         String rcaLog = FileUtils.readFileToString(rcaLogFile, StandardCharsets.UTF_8);
         return rcaLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }

   public Map<TestCase, List<RCALevel>> getRCATestcases() {
      CauseSearchFolders causeFolders = visualizationFolders.getPeassRCAFolders();
      File versionTreeFolder = new File(causeFolders.getRcaTreeFolder(), measurementConfig.getVersion());
      Map<TestCase, List<RCALevel>> testcases = new HashMap<>();
      if (versionTreeFolder.exists()) {
         for (File testcaseName : versionTreeFolder.listFiles()) {
            for (File jsonFileName : testcaseName.listFiles((FilenameFilter) new WildcardFileFilter("*.json"))) {
               try {
                  readRCATestcase(causeFolders, testcases, jsonFileName);
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         }
      }
      return testcases;
   }

   private void readRCATestcase(final CauseSearchFolders causeFolders, final Map<TestCase, List<RCALevel>> testcases, final File jsonFileName)
         throws IOException, JsonParseException, JsonMappingException {
      CauseSearchData data = Constants.OBJECTMAPPER.readValue(jsonFileName, CauseSearchData.class);
      TestCase test = new TestCase(data.getTestcase());

      boolean lastHadLogs = true;
      int levelId = 0;
      List<RCALevel> levels = new LinkedList<>();
      while (lastHadLogs) {
         List<LogFiles> currentFiles = new LinkedList<>();
         File logFolder = causeFolders.getExistingRCALogFolder(measurementConfig.getVersion(), test, levelId);
         tryLocalLogFolderVMIds(test, currentFiles, logFolder, causeFolders);
         if (currentFiles.size() > 0) {
            RCALevel level = new RCALevel(currentFiles);
            levels.add(level);
            levelId++;
         } else {
            lastHadLogs = false;
         }
      }

      testcases.put(test, levels);
   }
}
