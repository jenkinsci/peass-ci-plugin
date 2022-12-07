package de.dagere.peass.ci.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.config.FixedCommitConfig;

public class RCAMetadata {
   private final String actionName;
   private final String fileName;
   private final String predecessorFileName;
   private final String currentFileName;
   
   private final File rcaResults;
   private final FixedCommitConfig config;
   
   private String testclazz;
   private String method;
   
   public RCAMetadata(Change change, Entry<String, List<Change>> testcases, FixedCommitConfig config, File rcaResults) {
      this.rcaResults = rcaResults;
      this.config = config;
      testclazz = testcases.getKey();
      if (change.getParams() != null) {
         method = change.getMethod() + "(" + change.getParams() + ")";
         actionName = testclazz + "_" + change.getMethod() + "(" + change.getParams() + ")";
         fileName = testclazz + File.separator + change.getMethod() + "(" + change.getParams() + ")";
         predecessorFileName = testclazz + File.separator + change.getMethod() + "(" + change.getParams() + ")_" + config.getCommitOld();
         currentFileName = testclazz + File.separator + change.getMethod() + "(" + change.getParams() + ")_" + config.getCommit();
      } else {
         method = change.getMethod();
         actionName = testclazz + "_" + change.getMethod();
         fileName = testclazz + File.separator + change.getMethod();
         predecessorFileName = testclazz + File.separator + change.getMethod() + "_" + config.getCommitOld();
         currentFileName = testclazz + File.separator + change.getMethod() + "_" + config.getCommit();
      }
   }
   
   public void copyFiles(File commitVisualizationFolder) throws IOException {
      File mainJsFile = new File(commitVisualizationFolder, fileName + ".js");
      final File rcaDestFile = getRCAMainFile();
      FileUtils.copyFile(mainJsFile, rcaDestFile);
      
      File predecessorJsFile = new File(commitVisualizationFolder, fileName + "_" + config.getCommitOld() + ".js");
      
      if (predecessorJsFile.exists()) {
         final File predecessorDestFile = getPredecessorFile();
         FileUtils.copyFile(predecessorJsFile, predecessorDestFile);
      }
      
      File currentJsFile = new File(commitVisualizationFolder, fileName + "_" + config.getCommit() + ".js");
      if (currentJsFile.exists()) {
         final File currentDestFile = getCurrentFile();
         FileUtils.copyFile(currentJsFile, currentDestFile);
      }
   }

   public File getCurrentFile() {
      return new File(rcaResults, getCurrentFileName() + ".js");
   }

   public File getPredecessorFile() {
      return new File(rcaResults, getPredecessorFileName() + ".js");
   }
   
   public File getRCAMainFile() {
      final String destName = testclazz + "_" + method + ".js";
      final File rcaDestFile = new File(rcaResults, destName);
      return rcaDestFile;
   }

   public String getActionName() {
      return actionName;
   }

   public String getFileName() {
      return fileName;
   }

   public String getPredecessorFileName() {
      return predecessorFileName;
   }

   public String getCurrentFileName() {
      return currentFileName;
   }
}
