package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.CauseSearchFolders;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfiguration measurementConfig;

   public LogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfiguration measurementConfig) {
      this.visualizationFolders = visualizationFolders;
      this.measurementConfig = measurementConfig;

   }

   public Map<TestCase, List<LogFiles>> readAllTestcases(final Set<TestCase> tests) {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      for (TestCase testcase : tests) {
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
      File logFolder = folders.getExistingMeasureLogFolder(measurementConfig.getExecutionConfig().getVersion(), testcase);
      tryLocalLogFolderVMIds(testcase, currentFiles, logFolder, folders);
      logFiles.put(testcase, currentFiles);
   }

   private void tryLocalLogFolderVMIds(final TestCase testcase, final List<LogFiles> currentFiles, final File logFolder, final PeassFolders folders) {
      if (logFolder != null && logFolder.exists() && logFolder.isDirectory()) {
         LOG.debug("Log folder: {} {}", logFolder, logFolder.listFiles());
         int tryIndex = 0;
         String filenameSuffix = "log_" + testcase.getClazz() + File.separator + testcase.getMethod() + ".txt";
         File predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getExecutionConfig().getVersionOld() + File.separator + filenameSuffix);
         LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
         while (predecessorFile.exists()) {
            CorrectRunChecker checker = new CorrectRunChecker(testcase, tryIndex, measurementConfig, visualizationFolders);

            File currentFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getExecutionConfig().getVersion() + File.separator + filenameSuffix);
            LogFiles vmidLogFile = new LogFiles(predecessorFile, currentFile, checker.isPredecessorRunning(), checker.isCurrentRunning());
            currentFiles.add(vmidLogFile);

            tryIndex++;
            predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getExecutionConfig().getVersionOld() + File.separator + filenameSuffix);
            LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
         }
      } else {
         LOG.error("Log folder {} missing", logFolder);
      }

   }

   public String getMeasureLog() {
      File measureLogFile = visualizationFolders.getResultsFolders().getMeasurementLogFile(measurementConfig.getExecutionConfig().getVersion(), measurementConfig.getExecutionConfig().getVersionOld());
      try {
         if (measureLogFile.exists()) {
            LOG.debug("Reading {}", measureLogFile.getAbsolutePath());
            String rtsLog = FileUtils.readFileToString(measureLogFile, StandardCharsets.UTF_8);
            return rtsLog;
         } else {
            return "Measurement log not readable; file " + measureLogFile.getAbsolutePath() + " did not exist";
         }
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }

   public String getRCALog() {
      File rcaLogFile = visualizationFolders.getResultsFolders().getRCALogFile(measurementConfig.getExecutionConfig().getVersion(), measurementConfig.getExecutionConfig().getVersionOld());
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
      File versionTreeFolder = new File(causeFolders.getRcaTreeFolder(), measurementConfig.getExecutionConfig().getVersion());
      Map<TestCase, List<RCALevel>> testcases = new HashMap<>();
      File[] versionFiles = versionTreeFolder.listFiles();
      if (versionFiles != null) {
         for (File testcaseName : versionFiles) {
            File[] testcaseFiles = testcaseName.listFiles((FilenameFilter) new WildcardFileFilter("*.json"));
            if (testcaseFiles != null) {
               for (File jsonFileName : testcaseFiles) {
                  try {
                     LOG.debug("Loading: {}", jsonFileName.getAbsolutePath());
                     readRCATestcase(causeFolders, testcases, jsonFileName);
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
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
         File logFolder = causeFolders.getExistingRCALogFolder(measurementConfig.getExecutionConfig().getVersion(), test, levelId);
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
