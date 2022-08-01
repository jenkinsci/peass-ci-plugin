package de.dagere.peass.ci.peassOverview.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MeasurementMerger {
   
   private static final Logger LOG = LogManager.getLogger(MeasurementMerger.class);
   
   private File[] changeFile;
   private final SelectedTests selectedTests;
   
   @SuppressFBWarnings
   public MeasurementMerger(File[] changeFile, SelectedTests selectedTests) {
      this.changeFile = changeFile;
      this.selectedTests = selectedTests;
   }

   public void merge(ResultsFolders folders) throws IOException, StreamReadException, DatabindException, StreamWriteException {
      ProjectChanges changes = new ProjectChanges(new CommitComparatorInstance(selectedTests));
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
}
