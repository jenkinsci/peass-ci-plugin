package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.helper.HistogramReader;
import de.peass.ci.helper.HistogramValues;
import de.peass.ci.helper.RCAExecutor;
import de.peass.ci.helper.RCAVisualizer;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.analysis.ProjectStatistics;
import de.peass.measurement.rca.RCAStrategy;
import de.peass.utils.Constants;
import hudson.FilePath;
import hudson.model.Run;
import kieker.analysis.exception.AnalysisConfigurationException;

public class ExecutionPerformer {
   
   private final MeasurementConfiguration measurementConfig;
   private final List<String> includeList;
   private final boolean executeRCA;
   private final RCAStrategy measurementMode;
   
   public ExecutionPerformer(MeasurementConfiguration measurementConfig, List<String> includeList, boolean executeRCA, RCAStrategy measurementMode) {
      this.measurementConfig = measurementConfig;
      this.includeList = includeList;
      this.executeRCA = executeRCA;
      this.measurementMode = measurementMode;
   }

   public void performExecution(Run<?, ?> run, FilePath workspace) throws InterruptedException, IOException, JAXBException, XmlPullParserException, JsonParseException,
         JsonMappingException, AnalysisConfigurationException, ViewNotFoundException, Exception {

      final File projectFolder = new File(workspace.toString());
      final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, measurementConfig, 1, true);
      executor.execute(includeList);

      final HistogramReader histogramReader = new HistogramReader(executor);
      Map<String, HistogramValues> measurements = histogramReader.readMeasurements();

      final File changeFile = new File(executor.getLocalFolder(), "changes.json");
      final ProjectChanges changes;
      if (changeFile.exists()) {
         changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);

         if (executeRCA) {
            RCAExecutor rcaExecutor = new RCAExecutor(measurementConfig, executor, changes, measurementMode, includeList);
            rcaExecutor.executeRCAs();

            RCAVisualizer rcaVisualizer = new RCAVisualizer(executor, changes, run);
            rcaVisualizer.visualizeRCA();
         }
      } else {
         changes = new ProjectChanges();
      }

      final File statisticsFile = new File(executor.getLocalFolder(), "statistics.json");
      ProjectStatistics statistics = readStatistics(statisticsFile);

      final MeasureVersionAction action = new MeasureVersionAction(measurementConfig, changes.getVersion(measurementConfig.getVersion()), statistics, measurements);
      run.addAction(action);
   }

   private ProjectStatistics readStatistics(final File statisticsFile) throws IOException, JsonParseException, JsonMappingException {
      ProjectStatistics statistics;
      if (statisticsFile.exists()) {
         statistics = Constants.OBJECTMAPPER.readValue(statisticsFile, ProjectStatistics.class);
      } else {
         statistics = new ProjectStatistics();
      }
      return statistics;
   }
}
