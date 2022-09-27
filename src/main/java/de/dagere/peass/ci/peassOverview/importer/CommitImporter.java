package de.dagere.peass.ci.peassOverview.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

public class CommitImporter {
   private final File fullPeassFolder, workspaceFolder, projectResultsFolder;

   private final String commit, predecessor;

   private final String jenkinsProjectName;

   public CommitImporter(File fullPeassFolder, File workspaceFolder, File projectResultsFolder, String commit, String predecessor, String jenkinsProjectName) {
      this.fullPeassFolder = fullPeassFolder;
      this.workspaceFolder = workspaceFolder;
      this.projectResultsFolder = projectResultsFolder;
      this.commit = commit;
      this.predecessor = predecessor;
      this.jenkinsProjectName = jenkinsProjectName;
   }

   public void prepareData() throws IOException, StreamReadException, DatabindException, StreamWriteException {
      File fakeMeasurementFolder = new File(fullPeassFolder, "measurement_" + commit + "_" + predecessor);
      if (!fakeMeasurementFolder.mkdir() && !fakeMeasurementFolder.exists()) {
         throw new RuntimeException("Could not create " + fakeMeasurementFolder);
      }

      GitUtils.goToCommit(commit, workspaceFolder);

      importRCAData();

      importMeasurementFolder(fakeMeasurementFolder);

   }

   private void importRCAData() throws IOException {
      File jobCommitFolder = new File(fullPeassFolder, jenkinsProjectName + "_peass/rca/treeMeasurementResults/" + commit);
      if (!jobCommitFolder.mkdirs() && !jobCommitFolder.exists()) {
         throw new RuntimeException("Could not create " + jobCommitFolder);
      }
      File rcaContentFolder = new File(projectResultsFolder, "rca-results");
      File rcaCommitFolder = new File(rcaContentFolder, "treeMeasurementResults/" + commit);
      if (rcaCommitFolder.exists()) {
         importRCACommitFolder(jobCommitFolder, rcaCommitFolder);
      } else {
         File[] rcaFolders = rcaContentFolder.listFiles();
         if (rcaFolders != null) {
            for (File folderCandidate : rcaFolders) {
               if (folderCandidate.isDirectory()) {
                  File treeMeasurementResultCandidate = new File(folderCandidate, "treeMeasurementResults/" + commit);
                  if (treeMeasurementResultCandidate.exists()) {
                     importRCACommitFolder(jobCommitFolder, treeMeasurementResultCandidate);
                  }
               }
            }
         }
      }
   }

   private void importRCACommitFolder(File jobCommitFolder, File rcaCommitFolder) throws IOException {
      File[] clazzFolders = rcaCommitFolder.listFiles();
      if (clazzFolders != null) {
         for (File clazzFolder : clazzFolders) {
            File jobClazzFolder = new File(jobCommitFolder, clazzFolder.getName());
            FileUtils.copyDirectory(clazzFolder, jobClazzFolder);
         }
      }
      
      File parentFile = new File(fullPeassFolder, "rcaLogs");
      System.out.println("RCA log folder creation: " + parentFile.mkdirs());
      
      File rcaLogFile = new File(parentFile, commit + "_" + predecessor + ".txt");
      FileUtils.touch(rcaLogFile);
      System.out.println("Created: " + rcaLogFile);
   }

   private void importMeasurementFolder(File fakeMeasurementFolder)
         throws IOException, StreamReadException, DatabindException, StreamWriteException {
      File measurementResultFolder = new File(projectResultsFolder, "measurement-results");
      File measurementsFullFolder = new File(measurementResultFolder, "measurementsFull");
      if (measurementsFullFolder.exists()) {
         copyCommitData(commit, predecessor, fakeMeasurementFolder, measurementsFullFolder);
      } else {
         File[] chunkFolders = measurementResultFolder.listFiles((FilenameFilter) new WildcardFileFilter("chunk*"));
         if (chunkFolders != null) {
            for (File chunkFolder : chunkFolders) {
               File chunkMeasurementsFullFolder = new File(chunkFolder, "measurementsFull");
               copyCommitData(commit, predecessor, fakeMeasurementFolder, chunkMeasurementsFullFolder);
            }
         }
      }
   }

   private void copyCommitData(String commit, String predecessor, File fakeMeasurementFolder, File measurementsFullFolder)
         throws IOException, StreamReadException, DatabindException, StreamWriteException {
      File[] jsonFiles = measurementsFullFolder.listFiles();
      if (jsonFiles != null) {
         for (File jsonFile : jsonFiles) {
            if (jsonFile.getName().endsWith(".json")) {
               Kopemedata data = Constants.OBJECTMAPPER.readValue(jsonFile, Kopemedata.class);
               for (VMResultChunk chunk : data.getChunks()) {
                  Set<String> commits = new HashSet<>();
                  for (VMResult result : chunk.getResults()) {
                     commits.add(result.getCommit());
                  }
                  if (commits.size() == 2 && commits.contains(commit) && commits.contains(predecessor)) {
                     String clazzName = data.getClazz();
                     Kopemedata copiedData = new Kopemedata(clazzName);
                     copiedData.getMethods().add(new TestMethod(data.getFirstMethodResult().getMethod()));
                     copiedData.getFirstMethodResult().getDatacollectorResults().add(new DatacollectorResult(data.getFirstTimeDataCollector().getName()));
                     copiedData.getChunks().add(chunk);
                     File resultFile = new File(fakeMeasurementFolder, jsonFile.getName());
                     Constants.OBJECTMAPPER.writeValue(resultFile, copiedData);
                  }
               }
            }
         }
      }
   }
}
