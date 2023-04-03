package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.rts.RTSLogData;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.TraceWriter;

public class RTSLogFileCommitReader {

   private static final Logger LOG = LogManager.getLogger(RTSLogFileCommitReader.class);
   private final VisualizationFolderManager visualizationFolders;

   private final String commit;
   private final Map<TestMethodCall, RTSLogData> files = new LinkedHashMap<>();
   
   private final TestSet ignoredTests;
   private final TestSet removedTests;
   
   private File testClazzFolder;
   private File methodFile;
   private TestMethodCall test;

   public RTSLogFileCommitReader(final VisualizationFolderManager visualizationFolders, final String commit, 
         final TestSet ignoredTests, final TestSet removedTests) {
      this.visualizationFolders = visualizationFolders;
      this.commit = commit;
      this.ignoredTests = ignoredTests;
      this.removedTests = removedTests;
   }

   public Map<TestMethodCall, RTSLogData> getTestmethodLogs(final Map<File, String> testClazzFolders) {
      for (Entry<File, String> testClazzFolder : testClazzFolders.entrySet()) {
         getTestmethodLog(testClazzFolder.getKey(), testClazzFolder.getValue());
      }
      return files;
   }

   private Set<String> findIgnoredMethods(String clazzName) {
      if (ignoredTests == null)
         return Collections.emptySet();
      for (Entry<TestClazzCall, Set<String>> ignoredTest : ignoredTests.entrySet()) {
         if (ignoredTest.getKey().getClazz().equals(clazzName)) {
            return ignoredTest.getValue();
         }
      }
      return Collections.emptySet();
   }
   
   private Set<String> findRemovedMethods(String clazzName) {
      if (removedTests == null)
         return Collections.emptySet();
      for (Entry<TestClazzCall, Set<String>> removedTest : removedTests.entrySet()) {
         if (removedTest.getKey().getClazz().equals(clazzName)) {
            return removedTest.getValue();
         }
      }
      return Collections.emptySet();
   }

   private void getTestmethodLog(final File testClazzFolder, final String module) {
      LOG.debug("Looking for method files in {}", testClazzFolder.getAbsolutePath());
      File[] methodFiles;
      methodFiles = testClazzFolder.listFiles();
      if (methodFiles != null) {
         String clazz = extractClazzNameFromTestClazzFolder(testClazzFolder);
         Set<String> ignoredMethods = findIgnoredMethods(clazz);
         Set<String> removedMethods = findRemovedMethods(clazz);
         for (File methodFile : methodFiles) {
            LOG.debug("Looking for method log file in {}", methodFile.getAbsolutePath());
            if (!methodFile.isDirectory()) {
               this.testClazzFolder = testClazzFolder;
               this.methodFile = methodFile;
               String method = methodFile.getName().substring(0, methodFile.getName().length() - ".txt".length());
               test = new TestMethodCall(clazz, method, module);

               boolean ignored = ignoredMethods.contains(method);
               LOG.debug("Found method log file in {}, corresponding test was {}", methodFile.getAbsolutePath(), ignored ? "ignored/disabled" : " not ignored /disabled");
               boolean removed = removedMethods.contains(method);
               addMethodLog(ignored, removed);
            }
         }
      }
   }

   private String extractClazzNameFromTestClazzFolder(final File testClazzFolder) {
      return testClazzFolder.getName().substring("log_".length());
   }

   private void addMethodLog(final boolean ignored, boolean removed) {
      File clazzDir = visualizationFolders.getResultsFolders().getClazzDir(commit, test);
      File viewMethodDir = new File(clazzDir, test.getMethodWithParams());
      boolean foundAnyParameterized = false;

      if ((!viewMethodDir.exists())) {
         foundAnyParameterized = addParameterizedMethodLogs(clazzDir, null, ignored, removed);
      } else {
         foundAnyParameterized = addParameterizedMethodLogs(clazzDir, viewMethodDir, ignored, removed);
      }

      if (!foundAnyParameterized) {
         addRegularMethodLog(viewMethodDir, ignored, removed);
      }
   }

   private boolean addParameterizedMethodLogs(final File clazzDir, final File viewMethodDir, final boolean ignored, boolean removed) {
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
                  test = new TestMethodCall(test.getClazz(), test.getMethod(), test.getModule(), params);
                  boolean runWasSuccessful = checkRunWasSuccessful(viewMethodDir);
                  addMethodLogData(runWasSuccessful, false, ignored, removed);
               } else {
                  test = new TestMethodCall(test.getClazz(), test.getMethod(), test.getModule());
                  /*
                   * runWasSuccessful is always true in this case we can't use checkRunWasSuccessful because no viewMethodDir exists if TestCase isParameterizedWithoutIndex
                   */
                  addMethodLogData(true, true, ignored, removed);
               }
            }
         }
      }
      return foundAnyParameterized;
   }

   private void addRegularMethodLog(final File viewMethodDir, final boolean ignored, boolean removed) {
      boolean runWasSuccessful = checkRunWasSuccessful(viewMethodDir);
      addMethodLogData(runWasSuccessful, false, ignored, removed);
   }

   private boolean checkRunWasSuccessful(final File viewMethodDir) {
      File viewMethodFile = new File(viewMethodDir, TraceWriter.getShortCommit(commit) + TraceFileManager.TXT_ENDING);
      File viewMethodFileZip = new File(viewMethodDir, TraceWriter.getShortCommit(commit) + TraceFileManager.ZIP_ENDING);
      return (viewMethodFile.exists() || viewMethodFileZip.exists());
   }

   private void addMethodLogData(final boolean runWasSuccessful, final boolean isParameterizedWithoutIndex, final boolean ignored, boolean removed) {
      File cleanFile = new File(testClazzFolder, "clean" + File.separator + methodFile.getName());
      RTSLogData data = new RTSLogData(commit, methodFile, cleanFile, runWasSuccessful, isParameterizedWithoutIndex, ignored, removed);

      files.put(test, data);
      LOG.debug("Adding log: {}", test);
   }

}