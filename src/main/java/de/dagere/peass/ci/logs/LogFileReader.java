package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;

public class LogFileReader {
   private static final Logger LOG = LogManager.getLogger(LogFileReader.class);

   private final VisualizationFolderManager visualizationFolders;
   private final MeasurementConfiguration measurementConfig;

   public LogFileReader(final VisualizationFolderManager visualizationFolders, final MeasurementConfiguration measurementConfig) {
      this.visualizationFolders = visualizationFolders;
      this.measurementConfig = measurementConfig;
   }

   public Map<TestCase, List<LogFiles>> readAllTestcases(final ProjectStatistics statistics) {
      Map<TestCase, List<LogFiles>> logFiles = new HashMap<>();
      for (TestCase testcase : statistics.getStatistics().get(measurementConfig.getVersion()).keySet()) {
         readTestcase(visualizationFolders.getPeassFolders(), logFiles, testcase);
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
         LOG.debug("Reading ", measureLogFile.getAbsolutePath());
         String rtsLog = FileUtils.readFileToString(measureLogFile, StandardCharsets.UTF_8);
         return rtsLog;
      } catch (IOException e) {
         e.printStackTrace();
         return "Measurement log not readable";
      }
   }
}
