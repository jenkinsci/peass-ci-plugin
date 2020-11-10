package de.peass.ci;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcher;
import de.peass.measurement.rca.CauseSearcherComplete;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.visualization.VisualizeRCA;
import de.peran.measurement.analysis.ProjectStatistics;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.cli.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;
import jenkins.tasks.SimpleBuildStep;
import kieker.analysis.exception.AnalysisConfigurationException;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep {

   private int VMs;
   private int iterations;
   private int warmup;
   private int repetitions;
   private int timeout;
   private double significanceLevel;

   private int versionDiff;
   private boolean useGC;

   @DataBoundConstructor
   public MeasureVersionBuilder(String test) {
      System.out.println("Initializing" + test);
   }

   @Override
   public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
      final MeasurementConfiguration config = getConfig();

      Map<String, HistogramValues> measurements = new TreeMap<>();
      if (!workspace.exists()) {
         listener.getLogger().println("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().println("Executing on " + workspace.toString());
         PrintStream outOriginal = System.out;
         PrintStream errOriginal = System.err;
         final File projectFolder = new File(workspace.toString());
         final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, config, 1, true);
         ProjectChanges changes = null;
         try {
            System.setOut(listener.getLogger());
            System.setErr(listener.getLogger());

            executor.execute();
            
            File measurementsFullFolder = executor.getFolders().getFullMeasurementFolder();
            for (File xmlResultFile : measurementsFullFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
               Kopemedata data = XMLDataLoader.loadData(xmlResultFile);
               //This assumes measurements are only executed once; if this is not the case, the matching result would need to be searched
               final TestcaseType testcase = data.getTestcases().getTestcase().get(0);
               HistogramValues values = getHistogramValues(executor, testcase);
               measurements.put(data.getTestcases().getClazz() + "#" + testcase.getName(), values);
            }

            changes = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "changes.json"), ProjectChanges.class);

            executeRCAs(config, executor, changes);

            visualizeRCA(executor, changes, run);
         } catch (Throwable e) {
            e.printStackTrace();
         } finally {
            System.setOut(outOriginal);
            System.setErr(errOriginal);
         }
         ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "statistics.json"), ProjectStatistics.class);

         final MeasureVersionAction action = new MeasureVersionAction(config, changes, statistics, measurements);
         run.addAction(action);
      }
   }

   private HistogramValues getHistogramValues(final ContinuousExecutor executor, final TestcaseType testcase) {
      Chunk chunk = testcase.getDatacollector().get(0).getChunk().get(0);
      
      List<Double> old = new LinkedList<>();
      List<Double> current = new LinkedList<>();
      for (Result result : chunk.getResult()) {
         if (result.getVersion().getGitversion().equals(executor.getVersionOld())){
            old.add(result.getValue());
         }
         if (result.getVersion().getGitversion().equals(executor.getLatestVersion())) {
            current.add(result.getValue());
         }
      }
      HistogramValues values = new HistogramValues(current.stream().mapToDouble(i -> i).toArray(), 
            old.stream().mapToDouble(i -> i).toArray());
      return values;
   }

   private void visualizeRCA(final ContinuousExecutor executor, ProjectChanges changes, Run<?, ?> run) throws Exception {
      VisualizeRCA visualizer = new VisualizeRCA();
      visualizer.setData(new File[] { executor.getFolders().getFullMeasurementFolder().getParentFile() });
      System.out.println("Setting property folder: " + executor.getPropertyFolder());
      visualizer.setPropertyFolder(executor.getPropertyFolder());
      final File resultFolder = new File(executor.getLocalFolder(), "visualization");
      resultFolder.mkdirs();
      visualizer.setResultFolder(resultFolder);
      visualizer.call();

      File rcaResults = new File(run.getRootDir(), "rca_visualization");
      rcaResults.mkdirs();

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      File versionVisualizationFolder = new File(resultFolder, executor.getLatestVersion());

      createVisualizationActions(run, rcaResults, versionChanges, versionVisualizationFolder);
   }

   private void createVisualizationActions(Run<?, ?> run, File rcaResults, Changes versionChanges, File versionVisualizationFolder) throws IOException {
      System.out.println("Creating actions: " + versionChanges.getTestcaseChanges().size());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final String name = testcases.getKey() + "#" + change.getMethod();
            File htmlFile = new File(versionVisualizationFolder, name + ".html");
            System.out.println("Trying to move " + htmlFile.getAbsolutePath());
            if (htmlFile.exists()) {
               String destName = testcases.getKey() + "_" + change.getMethod() + ".html";
               File rcaDestFile = new File(rcaResults, destName);
               FileUtils.copyFile(htmlFile, rcaDestFile);

               System.out.println("Adding: " + rcaDestFile + " " + name);
               run.addAction(new RCAVisualizationAction(name, rcaDestFile));
            } else {
               System.out.println("An error occured: " + htmlFile.getAbsolutePath() + " not found");
            }
         }
      }
   }

   private void executeRCAs(final MeasurementConfiguration config, final ContinuousExecutor executor, ProjectChanges changes)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      config.setVersion(executor.getLatestVersion());
      config.setVersionOld(executor.getVersionOld());

      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            CauseSearchFolders folders = new CauseSearchFolders(executor.getProjectFolder());

            String onlyTestcaseName = (testcases.getKey().contains(".") ? testcases.getKey().substring(testcases.getKey().lastIndexOf('.') + 1) : testcases.getKey());
            final File expectedResultFile = new File(folders.getRcaTreeFolder(),
                  executor.getLatestVersion() + File.separator +
                        onlyTestcaseName + File.separator +
                        change.getMethod() + ".json");
            System.out.println("Testing " + expectedResultFile);
            if (!expectedResultFile.exists()) {
               executeRCA(config, executor, testcases, change);
            }
         }
      }
   }

   private void executeRCA(final MeasurementConfiguration config, final ContinuousExecutor executor, Entry<String, List<Change>> testcases, Change change)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      final TestCase testCase = new TestCase(testcases.getKey() + "#" + change.getMethod());
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, true, true, 5.0, true, 0.01, false, true);
      config.setUseKieker(true);

      final CauseSearchFolders alternateFolders = new CauseSearchFolders(executor.getFolders().getProjectFolder());
      final JUnitTestTransformer testtransformer = new JUnitTestTransformer(executor.getFolders().getProjectFolder(), config);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders);
      final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
      final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, config, alternateFolders);
      tester.search();
   }

   private MeasurementConfiguration getConfig() {
      final MeasurementConfiguration config = new MeasurementConfiguration(timeout, VMs, significanceLevel, 0.01);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      config.setEarlyStop(false);
      System.out.println("Building, iterations: " + iterations);
      return config;
   }

   public int getVMs() {
      return VMs;
   }

   @DataBoundSetter
   public void setVMs(int vMs) {
      VMs = vMs;
   }

   public int getIterations() {
      return iterations;
   }

   @DataBoundSetter
   public void setIterations(int iterations) {
      System.out.println("Setting: " + iterations);
      this.iterations = iterations;
   }

   public int getWarmup() {
      return warmup;
   }

   @DataBoundSetter
   public void setWarmup(int warmup) {
      this.warmup = warmup;
   }

   public int getRepetitions() {
      return repetitions;
   }

   @DataBoundSetter
   public void setRepetitions(int repetitions) {
      this.repetitions = repetitions;
   }

   public int getTimeout() {
      return timeout;
   }

   @DataBoundSetter
   public void setTimeout(int timeout) {
      this.timeout = timeout;
   }

   public double getSignificanceLevel() {
      return significanceLevel;
   }

   @DataBoundSetter
   public void setSignificanceLevel(double significanceLevel) {
      this.significanceLevel = significanceLevel;
   }

   public int getVersionDiff() {
      return versionDiff;
   }

   @DataBoundSetter
   public void setVersionDiff(int versionDiff) {
      this.versionDiff = versionDiff;
   }

   public boolean isUseGC() {
      return useGC;
   }

   @DataBoundSetter
   public void setUseGC(boolean useGC) {
      this.useGC = useGC;
   }

   @Symbol("measure")
   @Extension
   public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

      public FormValidation doCheckName(@QueryParameter String value,
            @QueryParameter boolean useFrench)
            throws IOException, ServletException {
         if (value.length() == 0)
            return FormValidation.error("Strange value: " + value);
         return FormValidation.ok();
      }

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> aClass) {
         return true;
      }

      @Override
      public String getDisplayName() {
         return Messages.MeasureVersion_DescriptorImpl_DisplayName();
      }

   }

}
