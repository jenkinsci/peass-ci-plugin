package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rca.RCALevel;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final PeassProcessConfiguration processConfig;
   private final MeasurementConfig measurementConfig;

   public LogFileReader(final VisualizationFolderManager visualizationFolders, final PeassProcessConfiguration processConfig) {
      this.visualizationFolders = visualizationFolders;
      this.processConfig = processConfig;
      this.measurementConfig = processConfig.getMeasurementConfig();

   }

   public Map<TestCase, List<LogFiles>> readAllTestcases(final Set<TestMethodCall> tests) {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      for (TestMethodCall testcase : tests) {
         readTestcase(visualizationFolders.getPeassFolders(), logFiles, testcase);
      }
      return logFiles;
   }

   public Map<TestCase, List<LogFiles>> readAllRCATestcases() {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      return logFiles;
   }

   private void readTestcase(final PeassFolders folders, final Map<TestCase, List<LogFiles>> logFiles, final TestMethodCall testcase) {
      LOG.info("Reading testcase " + testcase);

      File logFolder = folders.getExistingMeasureLogFolder(measurementConfig.getFixedCommitConfig().getCommit(), testcase);
      List<LogFiles> currentFiles = tryLocalLogFolderVMIds(testcase, logFolder, folders);
      logFiles.put(testcase, currentFiles);
   }

   private List<LogFiles> tryLocalLogFolderVMIds(final TestMethodCall testcase, final File logFolder, final PeassFolders folders) {
      List<LogFiles> currentFiles = new LinkedList<>();
      if (logFolder != null && logFolder.exists() && logFolder.isDirectory()) {
         LOG.debug("Log folder: {} {}", logFolder, logFolder.listFiles());

         int tryIndex = 0;
         String filenameSuffix = "log_" + testcase.getClazz() + File.separator + testcase.getMethodWithParams() + ".txt";
         File predecessorFile = getCommitFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getFixedCommitConfig().getCommitOld());

         LOG.debug("Trying whether {} exists {}", predecessorFile, predecessorFile.exists());
         while (predecessorFile.exists()) {
            CorrectRunChecker checker = new CorrectRunChecker(testcase, tryIndex, measurementConfig, visualizationFolders);

            File currentFile = getCommitFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getFixedCommitConfig().getCommit());
            LogFiles vmidLogFile = new LogFiles(predecessorFile, currentFile, checker.isPredecessorRunning(), checker.isCurrentRunning());
            currentFiles.add(vmidLogFile);

            tryIndex++;
            predecessorFile = getCommitFile(testcase, logFolder, tryIndex, filenameSuffix, measurementConfig.getFixedCommitConfig().getCommitOld());
            LOG.debug("Trying whether {} exists: {}", predecessorFile, predecessorFile.exists());
         }
      } else {
         LOG.info("Log folder {} not existing", logFolder);
      }
      return currentFiles;
   }

   private File getCommitFile(final TestCase testcase, final File logFolder, final int tryIndex, final String filenameSuffix, final String commit) {
      String vmFolderName = "vm_" + tryIndex + "_" + commit;
      File predecessorFile;
      if (testcase.getModule() != null) {
         predecessorFile = new File(logFolder, vmFolderName + File.separator + testcase.getModule() + File.separator + filenameSuffix);
      } else {
         predecessorFile = new File(logFolder, vmFolderName + File.separator + filenameSuffix);
      }
      return predecessorFile;
   }

   public String getMeasureLog() {
      File measureLogFile = visualizationFolders.getResultsFolders().getMeasurementLogFile(measurementConfig.getFixedCommitConfig().getCommit(),
            measurementConfig.getFixedCommitConfig().getCommitOld());
      try {
         if (measureLogFile.exists()) {
            LOG.debug("Reading {}", measureLogFile.getAbsolutePath());
            String measureLog = processConfig.getFileText(measureLogFile);
            return measureLog;
         } else {
            return "Measurement log not readable; file " + measureLogFile.getAbsolutePath() + " did not exist";
         }
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }

   public String getRCALog() {
      File rcaLogFile = visualizationFolders.getResultsFolders().getRCALogFile(measurementConfig.getFixedCommitConfig().getCommit(),
            measurementConfig.getFixedCommitConfig().getCommitOld());
      try {
         LOG.debug("Reading {}", rcaLogFile.getAbsolutePath());
         String rcaLog = processConfig.getFileText(rcaLogFile);
         return rcaLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }

   public Map<TestCase, List<RCALevel>> getRCATestcases() {
      CauseSearchFolders causeFolders = visualizationFolders.getPeassRCAFolders();
      File commitTreeFolder = new File(causeFolders.getRcaTreeFolder(), measurementConfig.getFixedCommitConfig().getCommit());
      Map<TestCase, List<RCALevel>> testcases = new HashMap<>();
      File[] commitFiles = commitTreeFolder.listFiles();
      if (commitFiles != null) {
         for (File testcaseName : commitFiles) {
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
      TestMethodCall test = data.getCauseConfig().getTestCase();

      boolean lastHadLogs = true;
      int levelId = 0;
      List<RCALevel> levels = new LinkedList<>();
      while (lastHadLogs) {
         File logFolder = causeFolders.getExistingRCALogFolder(measurementConfig.getFixedCommitConfig().getCommit(), test, levelId);
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
