package de.dagere.peass.ci.peassOverview.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitList;
import de.dagere.peass.vcs.GitCommit;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Imports existing measurement data from Peass CLI usage, which need to contains
 * <ul>
 * <li>staticTestSelection_$project.json</li>
 * <li>traceTestSelection_$project.json</li>
 * <li>statistics.json</li>
 * <li>changes.json</li>
 * </ul>
 * into a format readable by the Peass-CI plugin. It is assumed that statistics.json is in the same folder as changes.json.
 * 
 * After the data have been installed to the job, run one build and afterwards the data will be available and a creation of an peassOverview is possible.
 *
 */
public class ImportStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(ImportStarter.class);

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   protected File staticSelectionFile;

   @Option(names = { "-executionFile", "--executionFile" }, description = "Path to the executionfile (may be trace based selection or coverage selection file)", required = true)
   protected File executionFile;

   @Option(names = { "-changeFile", "--changeFile" }, description = "Path to the change file (normally changes.json)", required = true)
   protected File[] changeFile;

   @Option(names = { "-fullPeassFolder", "--fullPeassFolder" }, description = "Path to the fullPeassFolder of the project (normally $project_fullPeass)", required = true)
   protected File fullPeassFolder;

   public static void main(String[] args) {
      final CommandLine commandLine = new CommandLine(new ImportStarter());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      String projectFolderName = fullPeassFolder.getName().substring(0, fullPeassFolder.getName().length() - "_fullPeass".length());
      ResultsFolders folders = new ResultsFolders(fullPeassFolder, projectFolderName);
      LOG.debug("Project name: {}", projectFolderName);

      importSelectionFiles(folders);

      writeCommitList(folders);

      measurementMerger(folders);
      return null;

   }

   private void measurementMerger(ResultsFolders folders) throws IOException, StreamReadException, DatabindException, StreamWriteException {
      ProjectChanges changes = new ProjectChanges();
      ProjectStatistics statistics = new ProjectStatistics();
      for (File changeFileInstance : changeFile) {
         if (!changeFileInstance.exists()) {
            throw new RuntimeException("Import is only possible if changefile exists!");
         }

         readChangefile(changes, changeFileInstance);

         readStatisticsFile(statistics, changeFileInstance);
      }

      LOG.info("Writing to {}", folders.getChangeFile());
      Constants.OBJECTMAPPER.writeValue(folders.getChangeFile(), changes);

      LOG.info("Writing to {}", folders.getStatisticsFile());
      Constants.OBJECTMAPPER.writeValue(folders.getStatisticsFile(), statistics);
   }

   private void readStatisticsFile(ProjectStatistics statistics, File changeFileInstance) throws IOException, StreamReadException, DatabindException {
      File currentStatisticsFile = new File(changeFileInstance.getParentFile(), "statistics.json");
      ProjectStatistics currentStatistics = Constants.OBJECTMAPPER.readValue(currentStatisticsFile, ProjectStatistics.class);
      for (Entry<String, Map<TestCase, TestcaseStatistic>> commitEntry : currentStatistics.getStatistics().entrySet()) {
         for (Entry<TestCase, TestcaseStatistic> testEntry : commitEntry.getValue().entrySet()) {
            statistics.addMeasurement(commitEntry.getKey(), testEntry.getKey(), testEntry.getValue());
         }
      }
   }

   private void readChangefile(ProjectChanges changes, File changeFileInstance) throws IOException, StreamReadException, DatabindException {
      LOG.info("Reading from {}", changeFileInstance);
      ProjectChanges currentChanges = Constants.OBJECTMAPPER.readValue(changeFileInstance, ProjectChanges.class);

      for (Entry<String, Changes> commitChanges : currentChanges.getCommitChanges().entrySet()) {
         String commit = commitChanges.getKey();
         Map<TestCase, List<Change>> testcaseObjectChanges = commitChanges.getValue().getTestcaseObjectChanges();
         for (Entry<TestCase, List<Change>> testcase : testcaseObjectChanges.entrySet()) {
            for (Change change : testcase.getValue()) {
               changes.addChange(testcase.getKey(), commit, change);
            }
         }
      }
   }

   private void importSelectionFiles(ResultsFolders folders) throws IOException {
      LOG.info("Copying {} to {}", executionFile, folders.getTraceTestSelectionFile());
      FileUtils.copyFile(executionFile, folders.getTraceTestSelectionFile());

      if (staticSelectionFile == null) {
         String originalName = executionFile.getName().substring(ResultsFolders.TRACE_SELECTION_PREFIX.length(), executionFile.getName().length() - ".json".length());
         staticSelectionFile = new File(executionFile.getParentFile(), ResultsFolders.STATIC_SELECTION_PREFIX + originalName + ".json");
      }

      LOG.info("Copying {} to {}", staticSelectionFile, folders.getStaticTestSelectionFile());
      FileUtils.copyFile(staticSelectionFile, folders.getStaticTestSelectionFile());
   }

   private void writeCommitList(ResultsFolders folders) throws IOException, StreamReadException, DatabindException, StreamWriteException {
      CommitList commits = new CommitList();
      ExecutionData executionData = Constants.OBJECTMAPPER.readValue(folders.getTraceTestSelectionFile(), ExecutionData.class);
      for (String commitName : executionData.getCommitNames()) {
         final GitCommit gc = new GitCommit(commitName, "", "", "");
         commits.getCommits().add(gc);
      }
      Constants.OBJECTMAPPER.writeValue(folders.getCommitMetadataFile(), commits);
   }
}
