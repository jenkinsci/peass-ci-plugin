package de.dagere.peass.ci.rts;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.logs.rts.RTSLogSummary;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionVersion;
import de.dagere.peass.dependency.traces.diff.TraceFileUtil;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import hudson.model.Run;

public class RTSVisualizationCreator {

   private static final Logger LOG = LogManager.getLogger(RTSVisualizationCreator.class);

   private final ResultsFolders localWorkspace;
   private final PeassProcessConfiguration peassConfig;

   public RTSVisualizationCreator(final ResultsFolders localWorkspace, final PeassProcessConfiguration peassConfig) {
      this.localWorkspace = localWorkspace;
      this.peassConfig = peassConfig;
   }

   public void visualize(final Run<?, ?> run, final RTSLogSummary logSummary) {
      try {
         Map<String, List<String>> staticSelection = readStaticSelection(run);

         List<String> traceSelectedTests = readTraceBasedSelection(run);
         CoverageSelectionVersion coverageSelectedTests = readCoverageSelection(run);

         System.out.println("Selected: " + traceSelectedTests + " Coverage: " + coverageSelectedTests);

         FixedCommitConfig fixedCommitConfig = peassConfig.getMeasurementConfig().getFixedCommitConfig();
         RTSVisualizationAction rtsVisualizationAction = new RTSVisualizationAction(IdHelper.getId(), peassConfig.getDependencyConfig(), staticSelection, traceSelectedTests,
               coverageSelectedTests,
               fixedCommitConfig.getCommit(), fixedCommitConfig.getCommitOld(),
               logSummary);
         run.addAction(rtsVisualizationAction);

         for (String traceSelectedTest : traceSelectedTests) {
            visualizeTest(run, traceSelectedTest);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void visualizeTest(final Run<?, ?> run, final String traceSelectedTest) throws IOException {
      TestCase testcase = new TestCase(traceSelectedTest);
      File traceFolder = localWorkspace.getVersionDiffFolder(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());
      String traceSource = readText(testcase, traceFolder);

      RTSTraceAction traceAction = new RTSTraceAction(IdHelper.getId(), traceSelectedTest, traceSource);
      run.addAction(traceAction);
   }

   private String readText(TestCase testcase, File traceFolder) throws IOException {
      File traceFile = new File(traceFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + TraceFileManager.TXT_ENDING);
      System.out.println("Trace file: " + traceFile.getAbsolutePath());
      String traceSource = "";
      if (traceFile.exists()) {
         traceSource = TraceFileUtil.getText(traceFile).stream().collect(Collectors.joining("\n"));
      } else {
         File zipTraceFile = new File(traceFolder, testcase.getShortClazz() + "#" + testcase.getMethod() + TraceFileManager.ZIP_ENDING);
         if (zipTraceFile.exists()) {
            traceSource = TraceFileUtil.getText(zipTraceFile).stream().collect(Collectors.joining("\n"));
         }
      }
      return traceSource;
   }

   private List<String> readTraceBasedSelection(final Run<?, ?> run) throws IOException, JsonParseException, JsonMappingException {
      List<String> selectedTests = new LinkedList<>();
      File traceTestSelectionFile = localWorkspace.getTraceTestSelectionFile();
      if (traceTestSelectionFile.exists()) {
         ExecutionData traceSelections = Constants.OBJECTMAPPER.readValue(traceTestSelectionFile, ExecutionData.class);
         TestSet tests = traceSelections.getVersions().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());

         if (tests != null) {
            for (TestCase test : tests.getTests()) {
               selectedTests.add(test.toString());
            }
         }
      } else {
         LOG.info("File {} was not found, RTS execution info might be incomplete", traceTestSelectionFile.getAbsoluteFile());
      }
      return selectedTests;
   }

   private CoverageSelectionVersion readCoverageSelection(final Run<?, ?> run) throws IOException, JsonParseException, JsonMappingException {
      File coverageInfoFile = localWorkspace.getCoverageInfoFile();
      if (coverageInfoFile.exists()) {
         LOG.info("Reading {}", coverageInfoFile);
         CoverageSelectionInfo executions = Constants.OBJECTMAPPER.readValue(coverageInfoFile, CoverageSelectionInfo.class);
         CoverageSelectionVersion currentVersion = executions.getVersions().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());
         return currentVersion;
      } else {
         LOG.info("File {} was not found, RTS coverage based selection info might be incomplete", coverageInfoFile.getAbsoluteFile());
      }
      return null;
   }

   private Map<String, List<String>> readStaticSelection(final Run<?, ?> run) throws IOException, JsonParseException, JsonMappingException {
      Map<String, List<String>> staticSelection = new LinkedHashMap<String, List<String>>();
      File staticSelectionFile = localWorkspace.getStaticTestSelectionFile();
      if (staticSelectionFile.exists()) {
         StaticTestSelection staticTestSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
         VersionStaticSelection version = staticTestSelection.getVersions().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());

         if (version != null) {
            addVersionDataToChangeliste(staticSelection, version);
         } else {
            LOG.info("No change has been detected in " + peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());
         }

      } else {
         LOG.error("File {} was not found, RTS selection seems to not have worked at all", staticSelectionFile);
      }
      return staticSelection;
   }

   private void addVersionDataToChangeliste(final Map<String, List<String>> changesList, final VersionStaticSelection version) {
      for (Map.Entry<ChangedEntity, TestSet> entry : version.getChangedClazzes().entrySet()) {
         List<String> tests = new LinkedList<>();
         for (TestCase test : entry.getValue().getTests()) {
            tests.add(test.toString());
         }
         ChangedEntity changedClazz = entry.getKey();
         changesList.put(changedClazz.toString(), tests);
      }
   }
}
