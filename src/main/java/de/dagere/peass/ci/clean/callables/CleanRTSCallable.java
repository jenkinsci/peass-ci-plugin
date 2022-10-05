package de.dagere.peass.ci.clean.callables;

import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.folders.ResultsFolders;
import hudson.model.TaskListener;

public class CleanRTSCallable extends CleanCallable {

   private static final long serialVersionUID = 6370053198339266977L;
   
   public CleanRTSCallable(TaskListener listener) {
      super(listener);
   }

   public void cleanFolder(final ResultsFolders resultsFolders) throws IOException{
      deleteResultFiles(resultsFolders);
      deleteLogFolders(resultsFolders);
   }

   private static void deleteResultFiles(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getStaticTestSelectionFile());
      System.out.println("Success: " + resultsFolders.getStaticTestSelectionFile().delete());

      System.out.println("Deleting " + resultsFolders.getTraceTestSelectionFile());
      System.out.println("Success: " + resultsFolders.getTraceTestSelectionFile().delete());
      
      System.out.println("Deleting " + resultsFolders.getCoverageSelectionFile());
      System.out.println("Success: " + resultsFolders.getCoverageSelectionFile().delete());
      
      System.out.println("Deleting " + resultsFolders.getCoverageInfoFile());
      System.out.println("Success: " + resultsFolders.getCoverageInfoFile().delete());

      System.out.println("Deleting " + resultsFolders.getViewFolder());
      FileUtils.deleteDirectory(resultsFolders.getViewFolder());
      System.out.println("Deleting " + resultsFolders.getPropertiesFolder());
      FileUtils.deleteDirectory(resultsFolders.getPropertiesFolder());

   }

   private static void deleteLogFolders(final ResultsFolders resultsFolders) throws IOException {
      System.out.println("Deleting " + resultsFolders.getRtsLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getRtsLogFolder());
      System.out.println("Deleting " + resultsFolders.getSourceReadLogFolder());
      FileUtils.deleteDirectory(resultsFolders.getSourceReadLogFolder());
   }

}
