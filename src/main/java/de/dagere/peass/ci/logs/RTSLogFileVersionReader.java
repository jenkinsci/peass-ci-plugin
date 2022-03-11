package de.dagere.peass.ci.logs;

import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.TraceWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RTSLogFileVersionReader {

   private static final Logger LOG = LogManager.getLogger(RTSLogFileVersionReader.class);
   private final VisualizationFolderManager visualizationFolders;

   private final String version;
   private final Map<TestCase, RTSLogData> files = new LinkedHashMap<>();
   private File testClazzFolder;
   private File methodFile;
   private TestCase test;
   private boolean isParameterizedWithoutIndex = false;

   public RTSLogFileVersionReader(VisualizationFolderManager visualizationFolders, String version) {
      this.visualizationFolders = visualizationFolders;
      this.version = version;
   }

   public Map<TestCase, RTSLogData> getClazzLogs(Map<File, String> testClazzFolders) {
      testClazzFolders.forEach(this::getClazzLog);
      return files;
   }

   private void getClazzLog(File testClazzFolder, String module) {
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

               long count = Arrays.stream(methodFiles).map(File::getName).filter(methode -> methode.contains("(")).map(methode -> methode.substring(0, methode.indexOf("(")))
                     .map(methode -> methode.equals(test.getMethod())).count();
               isParameterizedWithoutIndex = count <= 1;

               addMethodLog();
            }
         }
      }
   }

   private void addMethodLog() {
      File clazzDir = visualizationFolders.getResultsFolders().getClazzDir(version, test);
      File viewMethodDir = new File(clazzDir, test.getMethodWithParams());
      boolean foundAnyParameterized = false;

      if ((!viewMethodDir.exists()) && !isParameterizedWithoutIndex) {
         foundAnyParameterized = addParameterizedMethodLogs(clazzDir);
      }
      if (!foundAnyParameterized) {
         addRegularMethodLog(viewMethodDir);
      }
   }

   private boolean addParameterizedMethodLogs(File clazzDir) {
      boolean foundAnyParameterized = false;
      File[] potentialParameterFiles = clazzDir.listFiles();
      if (potentialParameterFiles != null) {
         for (File potentialParameterizedFile : potentialParameterFiles) {
            String fileName = potentialParameterizedFile.getName();
            if (fileName.startsWith(test.getMethodWithParams() + "(")) {
               foundAnyParameterized = true;
               LOG.debug("Found parameterized trace file: {}", potentialParameterizedFile);

               String params = fileName.substring(test.getMethod().length() + 1, fileName.length() - 1);
               test = new TestCase(test.getClazz(), test.getMethod(), test.getModule(), params);
               addMethodLogData(true);
            }
         }
      }
      return foundAnyParameterized;
   }

   private void addRegularMethodLog(File viewMethodDir) {
      File viewMethodFile = new File(viewMethodDir, TraceWriter.getShortVersion(version));
      boolean runWasSuccessful = viewMethodFile.exists();
      addMethodLogData(runWasSuccessful);
   }

   private void addMethodLogData(boolean runWasSuccessful) {
      File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
      RTSLogData data = new RTSLogData(version, methodFile, cleanFile, runWasSuccessful, isParameterizedWithoutIndex);

      files.put(test, data);
      LOG.debug("Adding log: {}", test);
   }

}