package de.dagere.peass.ci.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitList;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.visualization.VisualizeRCAStarter;
import hudson.model.Result;
import hudson.model.Run;

public class RCAVisualizer {

   private static final Logger LOG = LogManager.getLogger(RCAVisualizer.class);

   private final PeassProcessConfiguration peassConfig;
   private final MeasurementConfig measurementConfig;
   private final VisualizationFolderManager visualizationFolders;
   private final ProjectChanges changes;
   private final Run<?, ?> run;
   private final RCAMapping mapping;

   public RCAVisualizer(final PeassProcessConfiguration peassConfig, final VisualizationFolderManager visualizationFolders, final ProjectChanges changes, final Run<?, ?> run) {
      this.peassConfig = peassConfig;
      this.measurementConfig = peassConfig.getMeasurementConfig();
      this.visualizationFolders = visualizationFolders;
      this.changes = changes;
      this.run = run;
      RCAMapping readMapping = new RCAMapping();
      try {
         File rcaMappingFile = visualizationFolders.getResultsFolders().getRCAMappingFile();
         if (rcaMappingFile.exists()) {
            readMapping = Constants.OBJECTMAPPER.readValue(rcaMappingFile, RCAMapping.class);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      mapping = readMapping;
   }

   public void visualizeRCA() throws IOException {
      final File visualizationFolder = visualizationFolders.getVisualizationFolder();

      VisualizeRCAStarter visualizer = preparePeassVisualizer(visualizationFolder);
      visualizer.call();

      File rcaResults = visualizationFolders.getRcaResultFolder();

      Changes commitChanges = changes.getCommitChanges(measurementConfig.getFixedCommitConfig().getCommit());
      File commitVisualizationFolder = new File(visualizationFolder, measurementConfig.getFixedCommitConfig().getCommit());

      createVisualizationActions(rcaResults, commitChanges, commitVisualizationFolder);
   }

   private VisualizeRCAStarter preparePeassVisualizer(final File resultFolder) {
      VisualizeRCAStarter visualizer = new VisualizeRCAStarter();
      visualizer.setCommit(measurementConfig.getFixedCommitConfig().getCommit());
      File dataFolder = visualizationFolders.getDataFolder();
      visualizer.setData(new File[] { dataFolder });
      File propertyFolder = visualizationFolders.getPropertyFolder();
      LOG.info("Setting property folder: {}", propertyFolder);
      visualizer.setPropertyFolder(propertyFolder);
      visualizer.setResultFolder(resultFolder);
      return visualizer;
   }

   private void createVisualizationActions(final File rcaResults, final Changes commitChanges, final File commitVisualizationFolder) throws IOException {
      String longestPrefix = getLongestPrefix(commitChanges.getTestcaseChanges().keySet());

      LOG.info("Creating actions: {}", commitChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : commitChanges.getTestcaseChanges().entrySet()) {
         final String clazzname = testcases.getKey();
         for (Change change : testcases.getValue()) {

            final File rcaTreeFile = getRcaTreeFile(clazzname, change);
            setUnstableIfNaNInRCA(rcaTreeFile);

            RCAMetadata metadata = new RCAMetadata(change, testcases, peassConfig.getMeasurementConfig().getFixedCommitConfig(), rcaResults);
            File jsFile = new File(commitVisualizationFolder, metadata.getFileName() + ".js");
            LOG.info("Trying to copy {} Exists: {}", jsFile.getAbsolutePath(), jsFile.exists());
            if (jsFile.exists()) {
               metadata.copyFiles(commitVisualizationFolder);
               createRCAAction(longestPrefix, testcases, change, metadata);
            } else {
               run.setResult(Result.UNSTABLE);
               LOG.error("An error occured: {} not found. Set buildstate to unstable.", jsFile.getAbsolutePath());
            }
         }
      }
   }

   private File getRcaTreeFile(final String clazzname, final Change change) {
      final String methodName = change.getMethod();
      final TestMethodCall testCall = TestMethodCall.createFromClassString(clazzname, methodName);
      final String commit = measurementConfig.getFixedCommitConfig().getCommit();
      return visualizationFolders.getPeassRCAFolders().getRcaTreeFile(commit, testCall);
   }

   private void setUnstableIfNaNInRCA(final File rcaTreeFile) throws IOException {
      if (rcaTreeFile.exists()) {
         CauseSearchData data = Constants.OBJECTMAPPER.readValue(rcaTreeFile, CauseSearchData.class);
         final boolean meanOldOrCurrentIsNaN = checkMeanOldOrCurrentIsNaN(data.getNodes().getStatistic());
         final boolean resultIsNullOrSuccess = checkResultIsNullOrSuccess();

         if (meanOldOrCurrentIsNaN && resultIsNullOrSuccess) {
            run.setResult(Result.UNSTABLE);
            LOG.warn("NaN for meanOld or meanCurrent was found in {}! Set buildstate to unstable.", rcaTreeFile.getAbsolutePath());
         }
      }
   }

   private boolean checkMeanOldOrCurrentIsNaN(final TestcaseStatistic testcaseStatistic) {
      return Double.isNaN(testcaseStatistic.getMeanCurrent()) ||
            Double.isNaN(testcaseStatistic.getMeanOld());
   }

   private boolean checkResultIsNullOrSuccess() {
      return run.getResult() == null || run.getResult() == Result.SUCCESS;
   }

   public void createRCAAction(final String longestPrefix, final Entry<String, List<Change>> testcases, final Change change, RCAMetadata metadata)
         throws IOException {
      final File rcaDestFile = metadata.getRCAMainFile();

      LOG.info("Adding: {} and {}", rcaDestFile, metadata.getActionName());
      final String displayName = metadata.getActionName().substring(longestPrefix.length());

      final String mainTreeJSContent = peassConfig.getFileText(rcaDestFile);
      final String predecessorTreeJSContent = metadata.getPredecessorFile().exists() ? peassConfig.getFileText(metadata.getPredecessorFile()) : null;
      final String currentTreeJSContent = metadata.getCurrentFile().exists() ? peassConfig.getFileText(metadata.getCurrentFile()) : null;

      TestMethodCall testMethodCall = TestMethodCall.createFromString(testcases.getKey() + "#" + change.getMethodWithParams());

      GitCommit commit = getCommit();

      RCAVisualizationAction visualizationAction = new RCAVisualizationAction(IdHelper.getId(), displayName, mainTreeJSContent, predecessorTreeJSContent, currentTreeJSContent,
            commit, testMethodCall.toString());
      run.addAction(visualizationAction);

      String url = run.getNumber() + "/" + visualizationAction.getUrlName();
      mapping.addMapping(measurementConfig.getFixedCommitConfig().getCommit(), testMethodCall, url);
      Constants.OBJECTMAPPER.writeValue(visualizationFolders.getResultsFolders().getRCAMappingFile(), mapping);
   }

   private GitCommit getCommit() throws IOException {
      String commitName = peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit();
      GitCommit commit;
      File commitMetadataFile = visualizationFolders.getResultsFolders().getCommitMetadataFile();
      if (commitMetadataFile.exists()) {
         CommitList commitList = Constants.OBJECTMAPPER.readValue(commitMetadataFile, CommitList.class);
         commit = commitList.getCommit(commitName);
      } else {
         commit = null;
      }
      return commit;
   }

   public static String getLongestPrefix(final Set<String> tests) {
      String longestPrefix;
      if (!tests.isEmpty()) {
         longestPrefix = tests.iterator().next();
      } else {
         longestPrefix = "";
      }
      for (final String clazz : tests) {
         String withoutClazzItself = clazz.substring(0, clazz.lastIndexOf('.') + 1);
         longestPrefix = StringUtils.getCommonPrefix(longestPrefix, withoutClazzItself);
      }
      return longestPrefix;
   }
}