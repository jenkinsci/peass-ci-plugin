package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final MeasurementConfiguration measurementConfig;

   public LogFileReader(final MeasurementConfiguration measurementConfig) {
      this.measurementConfig = measurementConfig;
   }

   public Map<TestCase, List<LogFiles>> readAllTestcases(final PeassFolders folders, final ProjectStatistics statistics) {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      for (TestCase testcase : statistics.getStatistics().get(measurementConfig.getVersion()).keySet()) {
         readTestcase(folders, logFiles, testcase);
      }
      return logFiles;
   }

   private void readTestcase(final PeassFolders folders, final Map<TestCase, List<LogFiles>> logFiles, final TestCase testcase) {
      LOG.info("Reading testcase " + testcase);
      List<LogFiles> currentFiles = new LinkedList<>();
      File logFolder = folders.getExistingLogFolder(measurementConfig.getVersion(), testcase);
      tryLocalLogFolderVMIds(testcase, currentFiles, logFolder);
      logFiles.put(testcase, currentFiles);
   }

   private void tryLocalLogFolderVMIds(final TestCase testcase, final List<LogFiles> currentFiles, final File logFolder) {
      LOG.debug("Log folder: {} {}", logFolder, logFolder.listFiles());
      int tryIndex = 0;
      String filenameSuffix = "log_" + testcase.getClazz() + File.separator + testcase.getMethod() + ".txt";
      File predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersionOld() + File.separator + filenameSuffix);
      LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
      while (predecessorFile.exists()) {
         File currentFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersion() + File.separator + filenameSuffix);
         LogFiles vmidLogFile = new LogFiles(predecessorFile, currentFile);
         currentFiles.add(vmidLogFile);

         tryIndex++;
         predecessorFile = new File(logFolder, "vm_" + tryIndex + "_" + measurementConfig.getVersionOld() + File.separator + filenameSuffix);
         LOG.debug("Trying whether {} exists", predecessorFile, predecessorFile.exists());
      }
   }
}
