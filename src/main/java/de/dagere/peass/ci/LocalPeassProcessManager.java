package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.helper.DefaultMeasurementVisualizer;
import de.dagere.peass.ci.helper.HistogramReader;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.ci.helper.RCAVisualizer;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.LogActionCreator;
import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.ci.remote.RTSResult;
import de.dagere.peass.ci.remote.RemoteMeasurer;
import de.dagere.peass.ci.remote.RemoteRCA;
import de.dagere.peass.ci.remote.RemoteRTS;
import de.dagere.peass.ci.rts.RTSVisualizationCreator;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.analysis.ProjectStatistics;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.utils.Constants;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.DirScanner;

public class LocalPeassProcessManager {

   private static final Logger LOG = LogManager.getLogger(LocalPeassProcessManager.class);

   private final FilePath workspace;
   private final File localWorkspace;
   private final TaskListener listener;
   private final PeassProcessConfiguration peassConfig;
   private final ResultsFolders results;
   private final LogActionCreator logActionCreator;

   public LocalPeassProcessManager(final PeassProcessConfiguration peassConfig, final FilePath workspace, final File localWorkspace, final TaskListener listener,
         final Run<?, ?> run) {
      this.peassConfig = peassConfig;
      this.workspace = workspace;
      this.localWorkspace = localWorkspace;
      this.listener = listener;
      this.results = new ResultsFolders(localWorkspace, run.getParent().getFullDisplayName());
      this.logActionCreator = new LogActionCreator(peassConfig, run, localWorkspace);
   }

   public Set<TestCase> rts() throws IOException, InterruptedException {
      RemoteRTS rts = new RemoteRTS(peassConfig, listener);
      RTSResult result = workspace.act(rts);
      peassConfig.getMeasurementConfig().setVersionOld(result.getVersionOld());
      copyFromRemote();
      return result.getTests();
   }

   public boolean measure(final Set<TestCase> tests) throws IOException, InterruptedException {
      final RemoteMeasurer remotePerformer = new RemoteMeasurer(peassConfig, listener, tests);
      boolean worked = workspace.act(remotePerformer);
      listener.getLogger().println("Measurement worked: " + worked);
      copyFromRemote();
      return worked;
   }

   public boolean rca(final ProjectChanges changes, final RCAStrategy rcaStrategy) throws IOException, InterruptedException, Exception {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(null, true, true, 0.01, false, true, rcaStrategy, 1);

      RemoteRCA remoteRCAExecutor = new RemoteRCA(peassConfig, causeSearcherConfig, changes, listener);
      boolean rcaWorked = workspace.act(remoteRCAExecutor);
      copyFromRemote();
      return rcaWorked;
   }

   public void copyFromRemote() throws IOException, InterruptedException {
      String remotePeassPath = ContinuousFolderUtil.getLocalFolder(new File(workspace.getRemote())).getPath();
      listener.getLogger().println("Remote Peass path: " + remotePeassPath);
      FilePath remotePeassFolder = new FilePath(workspace.getChannel(), remotePeassPath);
      DirScanner.Glob dirScanner = new DirScanner.Glob("**/*,**/.git/**", "", false);
      int count = remotePeassFolder.copyRecursiveTo(dirScanner, new FilePath(localWorkspace), "Copy including git folder");
      listener.getLogger().println("Copied " + count + " files from " + remotePeassFolder + " to " + localWorkspace.getAbsolutePath());
   }

   public void visualizeRTSResults(final Run<?, ?> run) throws IOException {
      RTSVisualizationCreator rtsVisualizationCreator = new RTSVisualizationCreator(results, peassConfig);
      rtsVisualizationCreator.visualize(run);
      if (peassConfig.isDisplayRTSLogs()) {
         logActionCreator.createRTSActions();
      }
   }

   public ProjectChanges visualizeMeasurementResults(final Run<?, ?> run)
         throws JAXBException, IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File dataFolder = results.getVersionFullResultsFolder(peassConfig.getMeasurementConfig().getVersion(), peassConfig.getMeasurementConfig().getVersionOld());
      final HistogramReader histogramReader = new HistogramReader(peassConfig.getMeasurementConfig(), dataFolder);
      final Map<String, HistogramValues> measurements = histogramReader.readMeasurements();

      final ProjectChanges changes = getChanges();

      final ProjectStatistics statistics = readStatistics();

      TrendFileUtil.persistTrend(run, localWorkspace, statistics);

      Changes versionChanges = changes.getVersion(peassConfig.getMeasurementConfig().getVersion());
      final MeasureVersionAction action = new MeasureVersionAction(peassConfig.getMeasurementConfig(), versionChanges, statistics, measurements, histogramReader.getUpdatedConfigurations());
      run.addAction(action);

      createPureMeasurementVisualization(run, dataFolder, measurements);

      if (peassConfig.isDisplayLogs()) {
         logActionCreator.createMeasurementActions(statistics);
      }

      return changes;
   }

   public void visualizeRCAResults(final Run<?, ?> run, final ProjectChanges changes) throws Exception, IOException {
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      final RCAVisualizer rcaVisualizer = new RCAVisualizer(peassConfig.getMeasurementConfig(), visualizationFolders, changes, run);
      rcaVisualizer.visualizeRCA();

      if (peassConfig.isDisplayRCALogs()) {
         logActionCreator.createRCAActions();
      }
   }

   private void createPureMeasurementVisualization(final Run<?, ?> run, final File dataFolder, final Map<String, HistogramValues> measurements) {
      VisualizationFolderManager visualizationFolders = new VisualizationFolderManager(localWorkspace, run);
      DefaultMeasurementVisualizer visualizer = new DefaultMeasurementVisualizer(dataFolder, peassConfig.getMeasurementConfig().getVersion(), run, visualizationFolders,
            measurements);
      visualizer.visualizeMeasurements();
   }

   private ProjectChanges getChanges() throws IOException, JsonParseException, JsonMappingException {
      final File changeFile = results.getChangeFile();
      final ProjectChanges changes;
      if (changeFile.exists()) {
         changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      } else {
         changes = new ProjectChanges();
      }
      return changes;
   }

   private ProjectStatistics readStatistics() throws IOException, JsonParseException, JsonMappingException {
      final File statisticsFile = results.getStatisticsFile();
      ProjectStatistics statistics;
      if (statisticsFile.exists()) {
         statistics = Constants.OBJECTMAPPER.readValue(statisticsFile, ProjectStatistics.class);
      } else {
         statistics = new ProjectStatistics();
      }
      return statistics;
   }
}
