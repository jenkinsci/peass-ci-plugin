package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.TraceWriter;

public class RTSLogFileVersionReader {

   private static final Logger LOG = LogManager.getLogger(RTSLogFileVersionReader.class);
   private final VisualizationFolderManager visualizationFolders;

   private final String version;
   private final Map<TestCase, RTSLogData> files = new LinkedHashMap<>();
   private File testClazzFolder;
   private File methodFile;
   private TestCase test;

   public RTSLogFileVersionReader(final VisualizationFolderManager visualizationFolders, final String version) {
      this.visualizationFolders = visualizationFolders;
      this.version = version;
   }

   public Map<TestCase, RTSLogData> getClazzLogs(final Map<File, String> testClazzFolders) {
      testClazzFolders.forEach(this::getClazzLog);
      return files;
   }

   private void getClazzLog(final File testClazzFolder, final String module) {
      LOG.debug("Looking for method files in {}", testClazzFolder.getAbsolutePath());
      File[] methodFiles;
      methodFiles = testClazzFolder.listFiles();
      if (methodFiles != null) {
         for (File methodFile : methodFiles) {
            LOG.debug("Looking for method log file in {}", methodFile.getAbsolutePath());
            if (!methodFile.isDirectory()) {
               this.testClazzFolder = testClazzFolder;
               this.methodFile = methodFile;
               String clazz = testClazzFolder.getName().substring("log_".length());
               String method = methodFile.getName().substring(0, methodFile.getName().length() - ".txt".length());
               test = new TestCase(clazz, method, module);

               addMethodLog();
            }
         }
      }
   }

   private void addMethodLog() {
      File clazzDir = visualizationFolders.getResultsFolders().getClazzDir(version, test);
      File viewMethodDir = new File(clazzDir, test.getMethodWithParams());
      boolean foundAnyParameterized = false;

      if ((!viewMethodDir.exists())) {
         foundAnyParameterized = addParameterizedMethodLogs(clazzDir);
      }
      if (!foundAnyParameterized) {
         addRegularMethodLog(viewMethodDir);
      }
   }

   private boolean addParameterizedMethodLogs(final File clazzDir) {
      boolean foundAnyParameterized = false;
      File[] potentialParameterFiles = clazzDir.listFiles();
      if (potentialParameterFiles != null) {
         for (File potentialParameterizedFile : potentialParameterFiles) {
            String fileName = potentialParameterizedFile.getName();
            if (fileName.startsWith(test.getMethodWithParams() + "(")) {
               foundAnyParameterized = true;
               LOG.debug("Found parameterized trace file: {}", potentialParameterizedFile);

               if (methodFile.getName().contains("(")) {
                  String params = fileName.substring(test.getMethod().length() + 1, fileName.length() - 1);
                  test = new TestCase(test.getClazz(), test.getMethod(), test.getModule(), params);
                  addMethodLogData(true, false);
               } else {
                  test = new TestCase(test.getClazz(), test.getMethod(), test.getModule());
                  addMethodLogData(true, true);
               }
            }
         }
      }
      return foundAnyParameterized;
   }

   private void addRegularMethodLog(final File viewMethodDir) {
      File viewMethodFile = new File(viewMethodDir, TraceWriter.getShortVersion(version) + TraceFileManager.TXT_ENDING);
      File viewMethodFileZip = new File(viewMethodDir, TraceWriter.getShortVersion(version) + TraceFileManager.ZIP_ENDING);
      boolean runWasSuccessful = viewMethodFile.exists() || viewMethodFileZip.exists();
      addMethodLogData(runWasSuccessful, false);
   }

   private void addMethodLogData(final boolean runWasSuccessful, final boolean isParameterizedWithoutIndex) {
      File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
      RTSLogData data = new RTSLogData(version, methodFile, cleanFile, runWasSuccessful, isParameterizedWithoutIndex);

      files.put(test, data);
      LOG.debug("Adding log: {}", test);
   }

}