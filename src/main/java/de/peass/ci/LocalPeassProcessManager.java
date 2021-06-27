package de.peass.ci;

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
import de.dagere.peass.ci.ContinuousFolderUtil;
import de.dagere.peass.ci.helper.HistogramReader;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.ci.helper.RCAVisualizer;
import de.dagere.peass.ci.persistence.TrendFileUtil;
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
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class LocalPeassProcessManager {

   private static final Logger LOG = LogManager.getLogger(LocalPeassProcessManager.class);

   private final FilePath workspace;
   private final File localWorkspace;
   private final TaskListener listener;
   private final PeassProcessConfiguration peassConfig;

   public LocalPeassProcessManager(final PeassProcessConfiguration peassConfig, final FilePath workspace, final File localWorkspace, final TaskListener listener) {
      this.peassConfig = peassConfig;
      this.workspace = workspace;
      this.localWorkspace = localWorkspace;
      this.listener = listener;
   }

   public boolean measure() throws IOException, InterruptedException {
      RemoteRTS rts = new RemoteRTS(peassConfig, listener);
      Set<TestCase> tests = workspace.act(rts);

      if (tests != null) {
         final RemoteMeasurer remotePerformer = new RemoteMeasurer(peassConfig, listener, tests);
         boolean worked = workspace.act(remotePerformer);
         listener.getLogger().println("Measurement worked: " + worked);
         return worked;
      } else {
         listener.getLogger().println("Regression test selection failed - please check log");
         return false;
      }
   }

   public void copyFromRemote() throws IOException, InterruptedException {
      String remotePeassPath = ContinuousFolderUtil.getLocalFolder(new File(workspace.getRemote())).getPath();
      listener.getLogger().println("Remote Peass path: " + remotePeassPath);
      FilePath remotePeassFolder = new FilePath(workspace.getChannel(), remotePeassPath);
      int count = remotePeassFolder.copyRecursiveTo(new FilePath(localWorkspace));
      listener.getLogger().println("Copied " + count + " files from " + remotePeassFolder + " to " + localWorkspace.getAbsolutePath());
   }

   public void visualizeDependencies(final Run<?, ?> run) {
      ResultsFolders results = new ResultsFolders(localWorkspace, run.getParent().getFullDisplayName());
      new RTSVisualizationCreator(results, peassConfig).visualize(run);
   }

   public ProjectChanges visualizeMeasurementData(final Run<?, ?> run)
         throws JAXBException, IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File dataFolder = new File(localWorkspace, peassConfig.getMeasurementConfig().getVersion() + "_" + peassConfig.getMeasurementConfig().getVersionOld());
      final HistogramReader histogramReader = new HistogramReader(peassConfig.getMeasurementConfig(), dataFolder);
      final Map<String, HistogramValues> measurements = histogramReader.readMeasurements();

      final File changeFile = new File(localWorkspace, "changes.json");
      final ProjectChanges changes;
      if (changeFile.exists()) {
         changes = Constants.OBJECTMAPPER.readValue(changeFile, ProjectChanges.class);
      } else {
         changes = new ProjectChanges();
      }

      final File statisticsFile = new File(localWorkspace, "statistics.json");
      final ProjectStatistics statistics = readStatistics(statisticsFile);

      TrendFileUtil.persistTrend(run, localWorkspace, statistics);

      Changes versionChanges = changes.getVersion(peassConfig.getMeasurementConfig().getVersion());
      final MeasureVersionAction action = new MeasureVersionAction(peassConfig.getMeasurementConfig(), versionChanges, statistics, measurements);
      run.addAction(action);

      return changes;
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

   public void rca(final Run<?, ?> run, final ProjectChanges changes, final RCAStrategy rcaStrategy) throws IOException, InterruptedException, Exception {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(null, true, true, 0.01, false, true, rcaStrategy, 1);

      RemoteRCA remoteRCAExecutor = new RemoteRCA(peassConfig, causeSearcherConfig, changes, listener);
      boolean rcaWorked = workspace.act(remoteRCAExecutor);
      if (!rcaWorked) {
         run.setResult(Result.FAILURE);
         return;
      }

      copyFromRemote();

      final RCAVisualizer rcaVisualizer = new RCAVisualizer(peassConfig.getMeasurementConfig(), localWorkspace, changes, run);
      rcaVisualizer.visualizeRCA();
   }

}
