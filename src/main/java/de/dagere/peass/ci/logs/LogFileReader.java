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
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfig measurementConfig;

   public LogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfig measurementConfig) {
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

      File logFolder = folders.getExistingMeasureLogFolder(measurementConfig.getExecutionConfig().getVersion(), testcase);
      List<LogFiles> currentFiles = tryLocalLogFolderVMIds(testcase, logFolder, folders);
      logFiles.put(testcase, currentFiles);
   }

   private List<LogFiles> tryLocalLogFolderVMIds(final TestCase testcase, final File logFolder, final PeassFolders folders) {
      List<LogFiles> currentFiles = new LinkedList<>();
      if (logFolder != null && logFolder.exists() && logFolder.isDirectory()) {
         LOG.debug("Log folder: {} {}", logFolder, logFolder.listFiles());

         int tryIndex = 0;
         String filenameSuffix = "log_" + testcase.getClazz() + File.separator + testcase.getMethodWithParams() + ".txt";
         File predecessorFile = getVersionFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getExecutionConfig().getVersionOld());

         LOG.debug("Trying whether {} exists {}", predecessorFile, predecessorFile.exists());
         while (predecessorFile.exists()) {
            CorrectRunChecker checker = new CorrectRunChecker(testcase, tryIndex, measurementConfig, visualizationFolders);

            File currentFile = getVersionFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getExecutionConfig().getVersion());
            LogFiles vmidLogFile = new LogFiles(predecessorFile, currentFile, checker.isPredecessorRunning(), checker.isCurrentRunning());
            currentFiles.add(vmidLogFile);

            tryIndex++;
            predecessorFile = getVersionFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getExecutionConfig().getVersionOld());
            LOG.debug("Trying whether {} exists: {}", predecessorFile, predecessorFile.exists());
         }
      } else {
         LOG.info("Log folder {} not existing", logFolder);
      }
      return currentFiles;
   }

   private File getVersionFile(final TestCase testcase, final File logFolder, final int tryIndex, final String filenameSuffix, final String version) {
      String vmFolderName = "vm_" + tryIndex + "_" + version;
      File predecessorFile;
      if (testcase.getModule() != null) {
         predecessorFile = new File(logFolder, vmFolderName + File.separator + testcase.getModule() + File.separator + filenameSuffix);
      } else {
         predecessorFile = new File(logFolder, vmFolderName + File.separator + filenameSuffix);
      }
      return predecessorFile;
   }

   public String getMeasureLog() {
      File measureLogFile = visualizationFolders.getResultsFolders().getMeasurementLogFile(measurementConfig.getExecutionConfig().getVersion(),
            measurementConfig.getExecutionConfig().getVersionOld());
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
      File rcaLogFile = visualizationFolders.getResultsFolders().getRCALogFile(measurementConfig.getExecutionConfig().getVersion(),
            measurementConfig.getExecutionConfig().getVersionOld());
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
      TestCase test = data.getCauseConfig().getTestCase();

      boolean lastHadLogs = true;
      int levelId = 0;
      List<RCALevel> levels = new LinkedList<>();
      while (lastHadLogs) {
         File logFolder = causeFolders.getExistingRCALogFolder(measurementConfig.getExecutionConfig().getVersion(), test, levelId);
         List<LogFiles> currentFiles = tryLocalLogFolderVMIds(test, logFolder, causeFolders);
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
