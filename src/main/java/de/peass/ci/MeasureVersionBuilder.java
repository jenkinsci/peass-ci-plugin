package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.helper.HistogramReader;
import de.peass.ci.helper.HistogramValues;
import de.peass.ci.helper.RCAVisualizer;
import de.peass.ci.persistence.TestcaseKeyDeserializer;
import de.peass.ci.persistence.TrendFileUtil;
import de.peass.ci.remote.RemoteMeasurer;
import de.peass.ci.remote.RemoteRCA;
import de.peass.ci.remote.RemoteVersionReader;
import de.peass.config.MeasurementConfiguration;
import de.peass.config.MeasurementStrategy;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.ProjectStatistics;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.RCAStrategy;
import de.peass.utils.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.tasks.SimpleBuildStep;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep, Serializable {

   private static final long serialVersionUID = -7455227251645979702L;

   static {
      Constants.OBJECTMAPPER.registerModules(new SimpleModule().addKeyDeserializer(TestCase.class, new TestcaseKeyDeserializer()));
   }

   private int VMs;
   private int iterations;
   private int warmup;
   private int repetitions;
   private int timeout = 5;
   private double significanceLevel = 0.01;

   private int versionDiff = 1;
   private boolean useGC;

   private String includes = "";
   private boolean executeRCA = true;
   private RCAStrategy measurementMode = RCAStrategy.LEVELWISE;
   private boolean executeParallel = false;
   private boolean executeBeforeClassInMeasurement = false;

   private boolean useSourceInstrumentation = true;
   private boolean useSampling = true;

   @DataBoundConstructor
   public MeasureVersionBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener) throws InterruptedException, IOException {
      if (!workspace.exists()) {
         throw new RuntimeException("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         final File localWorkspace = new File(run.getRootDir(), ".." + File.separator + ".." + File.separator + "peass-data").getCanonicalFile();
         printRunMetadata(run, workspace, listener, localWorkspace);

         if (!localWorkspace.exists()) {
            if (!localWorkspace.mkdirs()) {
               throw new RuntimeException("Was not able to create folder");
            }
         }

         try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
            final MeasurementConfiguration configWithRealGitVersions = generateMeasurementConfig(workspace, listener);
            boolean worked = measure(workspace, listener, configWithRealGitVersions);

            if (!worked) {
               run.setResult(Result.FAILURE);
               return;
            }

            copyFromRemote(workspace, listener, localWorkspace);

            ProjectChanges changes = visualizeMeasurementData(run, localWorkspace, configWithRealGitVersions);

            if (executeRCA) {
               rca(run, workspace, listener, localWorkspace, configWithRealGitVersions, changes);
            }
         } catch (Throwable e) {
            e.printStackTrace(listener.getLogger());
            e.printStackTrace();
            run.setResult(Result.FAILURE);
         }
      }
   }

   private void rca(final Run<?, ?> run, final FilePath workspace, final TaskListener listener, final File localWorkspace, final MeasurementConfiguration configWithRealGitVersions,
         final ProjectChanges changes) throws IOException, InterruptedException, Exception {
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(null, true, true, 0.01, false, true, measurementMode);

      RemoteRCA remoteRCAExecutor = new RemoteRCA(configWithRealGitVersions, causeSearcherConfig, changes, listener);
      boolean rcaWorked = workspace.act(remoteRCAExecutor);
      if (!rcaWorked) {
         run.setResult(Result.FAILURE);
         return;
      }

      copyFromRemote(workspace, listener, localWorkspace);

      final RCAVisualizer rcaVisualizer = new RCAVisualizer(configWithRealGitVersions, localWorkspace, changes, run);
      rcaVisualizer.visualizeRCA();
   }

   private boolean measure(final FilePath workspace, final TaskListener listener, final MeasurementConfiguration configWithRealGitVersions)
         throws IOException, InterruptedException {
      final RemoteMeasurer remotePerformer = new RemoteMeasurer(configWithRealGitVersions, listener);
      boolean worked = workspace.act(remotePerformer);
      listener.getLogger().println("First stage result: " + worked);
      return worked;
   }

   private MeasurementConfiguration generateMeasurementConfig(final FilePath workspace, final TaskListener listener) throws IOException, InterruptedException {
      final MeasurementConfiguration measurementConfig = getMeasurementConfig();
      System.out.println("Startig RemoteVersionReader");
      final RemoteVersionReader remoteVersionReader = new RemoteVersionReader(measurementConfig, listener);
      final MeasurementConfiguration configWithRealGitVersions = workspace.act(remoteVersionReader);
      listener.getLogger().println("Read version: " + configWithRealGitVersions.getVersion());
      return configWithRealGitVersions;
   }

   private void copyFromRemote(final FilePath workspace, final TaskListener listener, final File localWorkspace) throws IOException, InterruptedException {
      String remotePeassPath = ContinuousFolderUtil.getLocalFolder(new File(workspace.getRemote())).getPath();
      listener.getLogger().println("Remote Peass path: " + remotePeassPath);
      FilePath remotePeassFolder = new FilePath(workspace.getChannel(), remotePeassPath);
      int count = remotePeassFolder.copyRecursiveTo(new FilePath(localWorkspace));
      listener.getLogger().println("Copied " + count + " files from " + remotePeassFolder + " to " + localWorkspace.getAbsolutePath());
   }

   private ProjectChanges visualizeMeasurementData(final Run<?, ?> run, final File localWorkspace, final MeasurementConfiguration measurementConfig)
         throws JAXBException, IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File dataFolder = new File(localWorkspace, measurementConfig.getVersion() + "_" + measurementConfig.getVersionOld());
      final HistogramReader histogramReader = new HistogramReader(measurementConfig, dataFolder);
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

      final MeasureVersionAction action = new MeasureVersionAction(measurementConfig, changes.getVersion(measurementConfig.getVersion()), statistics, measurements);
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

   private void printRunMetadata(final Run<?, ?> run, final FilePath workspace, final TaskListener listener, final File localWorkspace) {
      listener.getLogger().println("Current Job: " + getJobName(run));
      listener.getLogger().println("Local workspace " + workspace.toString() + " Run dir: " + run.getRootDir() + " Local workspace: " + localWorkspace);
      listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
      listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);
      listener.getLogger().println("Strategy: " + measurementMode + " Source Instrumentation: " + useSourceInstrumentation + " Sampling: " + useSampling);
   }

   private String getJobName(final Run<?, ?> run) {
      return run.getParent().getFullDisplayName();
   }

   private List<String> getIncludeList() {
      List<String> includeList = new LinkedList<>();
      if (includes != null && includes.trim().length() > 0) {
         final String nonSpaceIncludes = includes.replaceAll(" ", "");
         for (String include : nonSpaceIncludes.split(";")) {
            includeList.add(include);
         }
      }
      return includeList;
   }

   private MeasurementConfiguration getMeasurementConfig() {
      if (significanceLevel == 0.0) {
         significanceLevel = 0.01;
      }
      final MeasurementConfiguration config = new MeasurementConfiguration(timeout * 60 * 1000, VMs, significanceLevel, 0.01);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      config.setEarlyStop(false);
      if (executeParallel) {
         System.out.println("Measuring parallel");
         config.setMeasurementStrategy(MeasurementStrategy.PARALLEL);
      } else {
         System.out.println("executeparallel is false");
      }
      if (useSourceInstrumentation) {
         config.setUseSourceInstrumentation(true);
         config.setUseSelectiveInstrumentation(true);
         config.setUseCircularQueue(true);
         if (useSampling) {
            config.setUseSampling(true);
            config.setRecord(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION);
         }
      }
      if (useSampling && !useSourceInstrumentation) {
         throw new RuntimeException("Sampling may only be used with source instrumentation currently.");
      }

      if (versionDiff <= 0) {
         throw new RuntimeException("The version difference should be at least 1, but was " + versionDiff);
      }
      config.setVersion("HEAD");
      config.setVersionOld("HEAD~" + versionDiff);

      config.setIncludes(getIncludeList());

      System.out.println("Building, iterations: " + iterations);
      return config;
   }

   public int getVMs() {
      return VMs;
   }

   @DataBoundSetter
   public void setVMs(final int vMs) {
      VMs = vMs;
   }

   public int getIterations() {
      return iterations;
   }

   @DataBoundSetter
   public void setIterations(final int iterations) {
      this.iterations = iterations;
   }

   public int getWarmup() {
      return warmup;
   }

   @DataBoundSetter
   public void setWarmup(final int warmup) {
      this.warmup = warmup;
   }

   public int getRepetitions() {
      return repetitions;
   }

   @DataBoundSetter
   public void setRepetitions(final int repetitions) {
      this.repetitions = repetitions;
   }

   public int getTimeout() {
      return timeout;
   }

   @DataBoundSetter
   public void setTimeout(final int timeout) {
      this.timeout = timeout;
   }

   public double getSignificanceLevel() {
      return significanceLevel;
   }

   @DataBoundSetter
   public void setSignificanceLevel(final double significanceLevel) {
      this.significanceLevel = significanceLevel;
   }

   public int getVersionDiff() {
      return versionDiff;
   }

   @DataBoundSetter
   public void setVersionDiff(final int versionDiff) {
      this.versionDiff = versionDiff;
   }

   public boolean isUseGC() {
      return useGC;
   }

   @DataBoundSetter
   public void setUseGC(final boolean useGC) {
      this.useGC = useGC;
   }

   public String getIncludes() {
      return includes;
   }

   @DataBoundSetter
   public void setIncludes(final String includes) {
      this.includes = includes;
   }

   public boolean isExecuteRCA() {
      return executeRCA;
   }

   @DataBoundSetter
   public void setExecuteRCA(final boolean executeRCA) {
      this.executeRCA = executeRCA;
   }

   public boolean isExecuteBeforeClassInMeasurement() {
      return executeBeforeClassInMeasurement;
   }

   public void setExecuteBeforeClassInMeasurement(final boolean executeBeforeClassInMeasurement) {
      this.executeBeforeClassInMeasurement = executeBeforeClassInMeasurement;
   }

   public RCAStrategy getMeasurementMode() {
      return measurementMode;
   }

   @DataBoundSetter
   public void setMeasurementMode(final RCAStrategy measurementMode) {
      this.measurementMode = measurementMode;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   @DataBoundSetter
   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }

   @DataBoundSetter
   public void setUseSampling(final boolean useSampling) {
      this.useSampling = useSampling;
   }

   public boolean isExecuteParallel() {
      return executeParallel;
   }

   @DataBoundSetter
   public void setExecuteParallel(final boolean executeParallel) {
      this.executeParallel = executeParallel;
   }

   @Symbol("measure")
   @Extension
   public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

      public FormValidation doCheckName(@QueryParameter final String value,
            @QueryParameter final boolean useFrench)
            throws IOException, ServletException {
         if (value.length() == 0)
            return FormValidation.error("Strange value: " + value);
         return FormValidation.ok();
      }

      @Override
      public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
         return true;
      }

      @Override
      public String getDisplayName() {
         return Messages.MeasureVersion_DescriptorImpl_DisplayName();
      }

      public ListBoxModel doFillMeasurementModeItems(@QueryParameter final String measurementMode) {
         ListBoxModel model = new ListBoxModel();
         model.add(new Option("Complete", "COMPLETE", "COMPLETE".equals(measurementMode)));
         model.add(new Option("Levelwise", "LEVELWISE", "LEVELWISE".equals(measurementMode)));
         model.add(new Option("Until Source Change (Early testing)", "UNTIL_SOURCE_CHANGE", "UNTIL_SOURCE_CHANGE".equals(measurementMode)));
         model.add(new Option("Until Structure Change (NOT IMPLEMENTED)", "UNTIL_STRUCTURE_CHANGE", "UNTIL_STRUCTURE_CHANGE".equals(measurementMode)));
         model.add(new Option("Constant Levels(NOT IMPLEMENTED)", "CONSTANT_LEVELS", "CONSTANT_LEVELS".equals(measurementMode)));
         return model;
      }
   }
}
