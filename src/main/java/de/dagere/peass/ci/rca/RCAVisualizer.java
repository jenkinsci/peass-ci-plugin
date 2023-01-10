package de.dagere.peass.ci.rca;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RCAVisualizationAction;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitList;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.visualization.VisualizeRCAStarter;
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

   public void visualizeRCA() throws Exception {
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
      LOG.info("Setting property folder: " + propertyFolder);
      visualizer.setPropertyFolder(propertyFolder);
      visualizer.setResultFolder(resultFolder);
      return visualizer;
   }

   private void createVisualizationActions(final File rcaResults, final Changes commitChanges, final File commitVisualizationFolder) throws IOException {
      String longestPrefix = getLongestPrefix(commitChanges.getTestcaseChanges().keySet());

      LOG.info("Creating actions: " + commitChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : commitChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            RCAMetadata metadata = new RCAMetadata(change, testcases, peassConfig.getMeasurementConfig().getFixedCommitConfig(), rcaResults);
            File jsFile = new File(commitVisualizationFolder, metadata.getFileName() + ".js");
            LOG.info("Trying to copy {} Exists: {}", jsFile.getAbsolutePath(), jsFile.exists());
            if (jsFile.exists()) {
               metadata.copyFiles(commitVisualizationFolder);
               createRCAAction(rcaResults, longestPrefix, testcases, change, metadata);
            } else {
               LOG.error("An error occured: " + jsFile.getAbsolutePath() + " not found");
            }
         }
      }
   }

   public void createRCAAction(final File rcaResults, final String longestPrefix, final Entry<String, List<Change>> testcases, final Change change, RCAMetadata metadata)
         throws IOException {
      final File rcaDestFile = metadata.getRCAMainFile();

      LOG.info("Adding: " + rcaDestFile + " " + metadata.getActionName());
      final String displayName = metadata.getActionName().substring(longestPrefix.length());

      final String mainTreeJSContent = peassConfig.getLogText(rcaDestFile);
      final String predecessorTreeJSContent = metadata.getPredecessorFile().exists() ? peassConfig.getLogText(metadata.getPredecessorFile()) : null;
      final String currentTreeJSContent = metadata.getCurrentFile().exists() ? peassConfig.getLogText(metadata.getCurrentFile()) : null;
      
      TestMethodCall testMethodCall = TestMethodCall.createFromString(testcases.getKey() + "#" + change.getMethodWithParams());
      
      GitCommit commit = getCommit();
      
      RCAVisualizationAction visualizationAction = new RCAVisualizationAction(IdHelper.getId(), displayName, mainTreeJSContent, predecessorTreeJSContent, currentTreeJSContent, commit, testMethodCall.toString());
      run.addAction(visualizationAction);
      
      String url = run.getNumber() + "/" + visualizationAction.getUrlName();
      mapping.addMapping(measurementConfig.getFixedCommitConfig().getCommit(), testMethodCall, url);
      Constants.OBJECTMAPPER.writeValue(visualizationFolders.getResultsFolders().getRCAMappingFile(), mapping);
   }

   private GitCommit getCommit() throws IOException, StreamReadException, DatabindException {
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
      if (tests.size() > 0) {
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