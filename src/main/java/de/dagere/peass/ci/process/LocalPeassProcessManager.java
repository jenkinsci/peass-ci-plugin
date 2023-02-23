package de.dagere.peass.ci.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.ContinuousFolderUtil;
import de.dagere.peass.ci.MeasurementOverviewAction;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RTSResult;
import de.dagere.peass.ci.helper.DefaultMeasurementVisualizer;
import de.dagere.peass.ci.helper.HistogramReader;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.LogActionCreator;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.ci.persistence.TrendFileUtil;
import de.dagere.peass.ci.rca.RCAVisualizer;
import de.dagere.peass.ci.remote.RCAResult;
import de.dagere.peass.ci.remote.RemoteMeasurer;
import de.dagere.peass.ci.remote.RemoteRCA;
import de.dagere.peass.ci.remote.RemoteRTS;
import de.dagere.peass.ci.rts.RTSVisualizationCreator;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.DirScanner;

public class LocalPeassProcessManager {

   private final FilePath workspace;
   private final File localWorkspace;
   private final TaskListener listener;
   private final PeassProcessConfiguration peassConfig;
   private final ResultsFolders results;
   private final LogActionCreator logActionCreator;
   private final VisualizationFolderManager visualizationFolders;

   public LocalPeassProcessManager(final PeassProcessConfiguration peassConfig, final FilePath workspace, final File localWorkspace, final TaskListener listener,
         final Run<?, ?> run) {
      this.peassConfig = peassConfig;
      this.workspace = workspace;
      this.localWorkspace = localWorkspace;
      this.listener = listener;
      String projectName = new File(workspace.getRemote()).getName();
      this.results = new ResultsFolders(localWorkspace, projectName);
      visualizationFolders = new VisualizationFolderManager(localWorkspace, projectName, run);
      this.logActionCreator = new LogActionCreator(peassConfig, run, visualizationFolders);

   }

   public AggregatedRTSResult rts() throws IOException, InterruptedException {
      RemoteRTS rts = new RemoteRTS(peassConfig, listener);
      RTSResult result = workspace.act(rts);
      copyFromRemote();
      if (result != null) {
         String commitOld = result.getCommitOld();
         listener.getLogger().println("Setting predecessor commit, obtained by RTS: " + commitOld);
         peassConfig.getMeasurementConfig().getFixedCommitConfig().setCommitOld(commitOld);
      }
      if (peassConfig.isDisplayRTSLogs()) {
         return displayRTSLogs(result);
      }
      if (result != null && result.getTests() != null) {
         AggregatedRTSResult aggregatedRTSResult = new AggregatedRTSResult(null, result);
         return aggregatedRTSResult;
      } else {
         return null;
      }

   }

   private AggregatedRTSResult displayRTSLogs(RTSResult result) throws IOException {
      RTSInfos infos = RTSInfos.readInfosFromFolders(results, peassConfig);
      RTSLogSummary summary = logActionCreator.createRTSActions(infos);
      AggregatedRTSResult aggregatedRTSResult = new AggregatedRTSResult(summary, result);
      return aggregatedRTSResult;
   }

   public boolean measure(final Set<TestMethodCall> tests) throws IOException, InterruptedException {
      final RemoteMeasurer remotePerformer = new RemoteMeasurer(peassConfig, listener, tests);
      boolean worked = workspace.act(remotePerformer);
      listener.getLogger().println("Measurement worked: " + worked);
      copyFromRemote();
      if (peassConfig.isDisplayLogs()) {
         logActionCreator.createMeasurementActions(tests);
      }
      return worked;
   }

   public boolean rca(final ProjectChanges changes, CauseSearcherConfig causeSearcherConfig) throws IOException, InterruptedException {
      RemoteRCA remoteRCAExecutor = new RemoteRCA(peassConfig, causeSearcherConfig, changes, listener);
      RCAResult result = workspace.act(remoteRCAExecutor);
      copyFromRemote();
      if (result.getFailedTests().size() > 0) {
         return false;
      }
      return result.isSuccess();
   }

   public void copyFromRemote() throws IOException, InterruptedException {
      String remotePeassPath = ContinuousFolderUtil.getLocalFolder(new File(workspace.getRemote())).getPath();
      listener.getLogger().println(Arrays.toString(new RuntimeException().getStackTrace()));
      listener.getLogger().println("Remote Peass path: " + remotePeassPath);
      FilePath remotePeassFolder = new FilePath(workspace.getChannel(), remotePeassPath);

      FileUtils.deleteDirectory(localWorkspace);
      if (!localWorkspace.mkdirs()) {
         listener.getLogger().println("Could not re-create " + localWorkspace);
      }
      
      DirScanner.Glob dirScanner = new DirScanner.Glob("**/*,**/.git/**", "", false);
      int count = remotePeassFolder.copyRecursiveTo(dirScanner, new FilePath(localWorkspace), "Copy including git folder");
      listener.getLogger().println("Copied " + count + " files from " + remotePeassFolder + " to " + localWorkspace.getAbsolutePath());
   }

   public void visualizeRTSResults(final Run<?, ?> run, final RTSLogSummary logSummary) {
      RTSVisualizationCreator rtsVisualizationCreator = new RTSVisualizationCreator(results, peassConfig);
      rtsVisualizationCreator.visualize(run, logSummary);
   }

   public ProjectChanges visualizeMeasurementResults(final Run<?, ?> run) {
      File dataFolder = results.getCommitFullResultsFolder(peassConfig.getMeasurementConfig());
      final HistogramReader histogramReader = new HistogramReader(peassConfig.getMeasurementConfig(), dataFolder);
      final Map<String, HistogramValues> measurements = histogramReader.readMeasurements();

      final ProjectChanges changes = getChanges();

      final ProjectStatistics statistics = readStatistics();

      TrendFileUtil.persistTrend(run, localWorkspace, statistics);

      Map<String, TestcaseStatistic> noWarmupStatistics = createPureMeasurementVisualization(run, dataFolder, measurements);

      Changes commitChanges = changes.getCommitChanges(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());

      final MeasurementOverviewAction action = new MeasurementOverviewAction(IdHelper.getId(), peassConfig.getMeasurementConfig(), commitChanges, statistics,
            noWarmupStatistics, measurements, histogramReader.getUpdatedConfigurations());
      run.addAction(action);

      return changes;
   }

   public void visualizeRCAResults(final Run<?, ?> run, final ProjectChanges changes) throws IOException {
      final RCAVisualizer rcaVisualizer = new RCAVisualizer(peassConfig, visualizationFolders, changes, run);
      rcaVisualizer.visualizeRCA();

      if (peassConfig.isDisplayRCALogs()) {
         logActionCreator.createRCAActions();
      }
   }

   private Map<String, TestcaseStatistic> createPureMeasurementVisualization(final Run<?, ?> run, final File dataFolder, final Map<String, HistogramValues> measurements) {
      DefaultMeasurementVisualizer visualizer = new DefaultMeasurementVisualizer(dataFolder, peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit(), run,
            visualizationFolders,
            measurements.keySet());
      visualizer.visualizeMeasurements();
      Map<String, TestcaseStatistic> noWarmupStatistics = visualizer.getNoWarmupStatistics();
      return noWarmupStatistics;
   }

   private ProjectChanges getChanges() {
      final File changeFile = results.getChangeFile();
      final ProjectChanges changes;
      if (changeFile.exists()) {
         try {
            changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         changes = new ProjectChanges();
      }
      return changes;
   }

   public ProjectStatistics readStatistics() {
      final File statisticsFile = results.getStatisticsFile();
      ProjectStatistics statistics;
      if (statisticsFile.exists()) {
         try {
            statistics = Constants.OBJECTMAPPER.readValue(statisticsFile, ProjectStatistics.class);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         statistics = new ProjectStatistics();
      }
      return statistics;
   }
}
