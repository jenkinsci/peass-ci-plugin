package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.logs.rts.AggregatedRTSResult;
import de.dagere.peass.ci.persistence.TestcaseKeyDeserializer;
import de.dagere.peass.ci.process.IncludeExcludeParser;
import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.ci.process.LocalPeassProcessManager;
import de.dagere.peass.ci.remote.RemoteVersionReader;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.measurement.rca.RCAStrategy;
import de.dagere.peass.utils.Constants;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep, Serializable {

   public static final String PEASS_FOLDER_NAME = "peass-data";

   private static final long serialVersionUID = -7455227251645979702L;

   static {
      SimpleModule keyDeserializer = new SimpleModule().addKeyDeserializer(TestCase.class, new TestcaseKeyDeserializer());
      Constants.OBJECTMAPPER.registerModules(keyDeserializer);
   }

   private int VMs = 30;
   private int iterations = 5;
   private int warmup = 5;
   private int repetitions = 1000000;
   private int timeout = 5;
   
   private boolean executeRCA = true;
   private boolean executeParallel = false;
   private String credentialsId;
   
   private int kiekerWaitTime = 10;
   private long kiekerQueueSize = 10000000;
   private double significanceLevel = 0.01;
   private boolean redirectToNull = true;
   private boolean showStart = false;

   private boolean nightlyBuild = true;
   private int versionDiff = 1;
   private int traceSizeInMb = 100;

   private boolean displayRTSLogs = true;
   private boolean displayLogs = true;
   private boolean displayRCALogs = true;
   private boolean generateCoverageSelection = false;
   private boolean useGC;
   private boolean measureJMH;

   private String includes = "";
   private String excludes = "";
   private String properties = "";
   private String testGoal = "test";
   private String pl = "";
   
   private RCAStrategy measurementMode = RCAStrategy.UNTIL_SOURCE_CHANGE;
   
   private boolean executeBeforeClassInMeasurement = false;
   private boolean onlyMeasureWorkload = false;
   private boolean onlyOneCallRecording = false;

   private boolean updateSnapshotDependencies = false;
   private boolean removeSnapshots = false;
   private boolean useAlternativeBuildfile = false;
   private boolean excludeLog4j = false;

   private boolean useSourceInstrumentation = true;
   private boolean useAggregation = true;
   private boolean createDefaultConstructor = false;

   private boolean redirectSubprocessOutputToFile = true;

   private String testTransformer = "de.dagere.peass.testtransformation.JUnitTestTransformer";
   private String testExecutor = "default";

   private String clazzFolders = "src/main/java:src/java";
   private String testClazzFolders = "src/test/java:src/test";

   private boolean failOnRtsError = false;

   private String excludeForTracing = "";

   

   @DataBoundConstructor
   public MeasureVersionBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final EnvVars env, final Launcher launcher, final TaskListener listener)
         throws InterruptedException, IOException {
      if (!workspace.exists()) {
         throw new RuntimeException("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         final File localWorkspace = new File(run.getRootDir(), ".." + File.separator + ".." + File.separator + PEASS_FOLDER_NAME).getCanonicalFile();
         printRunMetadata(run, workspace, listener, localWorkspace);

         if (!localWorkspace.exists()) {
            if (!localWorkspace.mkdirs()) {
               throw new RuntimeException("Was not able to create folder");
            }
         }

         Pattern patternForBuild = getMaskingPattern(listener.getLogger());

         try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
            PeassProcessConfiguration peassConfig = buildConfiguration(workspace, env, listener, patternForBuild);
            boolean versionIsUsable = checkVersion(run, listener, peassConfig);
            if (versionIsUsable) {
               runAllSteps(run, workspace, listener, localWorkspace, peassConfig);
            }
         } catch (Throwable e) {
            e.printStackTrace(listener.getLogger());
            e.printStackTrace();
            run.setResult(Result.FAILURE);
         }
      }
   }

   private Pattern getMaskingPattern(final PrintStream logger) {
      Pattern patternForBuild = null;
      if (credentialsId != null && !credentialsId.equals("")) {
         StandardUsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
               CredentialsProvider.lookupCredentials(
                     StandardUsernamePasswordCredentials.class,
                     Jenkins.get(),
                     ACL.SYSTEM,
                     Collections.emptyList()),
               CredentialsMatchers.allOf(
                     CredentialsMatchers.withId(credentialsId),
                     CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));

         if (credential != null) {
            String patternString = credential.getUsername() + "|" + credential.getPassword().getPlainText();
            patternForBuild = Pattern.compile(patternString);
         } else {
            logger.println("Could not find credential with name " + credentialsId);
         }
      }
      return patternForBuild;
   }

   private boolean checkVersion(final Run<?, ?> run, final TaskListener listener, final PeassProcessConfiguration peassConfig) {
      boolean versionIsUsable;
      String version = peassConfig.getMeasurementConfig().getExecutionConfig().getVersion();
      String versionOld = peassConfig.getMeasurementConfig().getExecutionConfig().getVersionOld();
      if (version.equals(versionOld)) {
         listener.getLogger().print("Version " + version + " equals " + versionOld + "; please check your configuration");
         run.setResult(Result.FAILURE);
         versionIsUsable = false;
      } else {
         versionIsUsable = true;
      }
      return versionIsUsable;
   }

   private void runAllSteps(final Run<?, ?> run, final FilePath workspace, final TaskListener listener, final File localWorkspace, final PeassProcessConfiguration peassConfig)
         throws IOException, InterruptedException, JAXBException, JsonParseException, JsonMappingException, JsonGenerationException, Exception {
      final LocalPeassProcessManager processManager = new LocalPeassProcessManager(peassConfig, workspace, localWorkspace, listener, run);

      AggregatedRTSResult tests = processManager.rts();
      listener.getLogger().println("Tests: " + tests);
      if (tests == null || !tests.getResult().isRunning()) {
         run.setResult(Result.FAILURE);
         return;
      }

      if (failOnRtsError && tests.isRtsError()) {
         run.setResult(Result.FAILURE);
         return;
      }

      processManager.visualizeRTSResults(run, tests.getLogSummary());

      if (tests.getResult().getTests().size() > 0) {
         measure(run, processManager, tests.getResult().getTests());
      } else {
         listener.getLogger().println("No tests selected; no measurement executed");
      }
   }

   private void measure(final Run<?, ?> run, final LocalPeassProcessManager processManager, final Set<TestCase> tests)
         throws IOException, InterruptedException, JAXBException, JsonParseException, JsonMappingException, JsonGenerationException, Exception {
      boolean worked = processManager.measure(tests);
      if (!worked) {
         run.setResult(Result.FAILURE);
         return;
      }
      ProjectChanges changes = processManager.visualizeMeasurementResults(run);

      if (executeRCA) {
         boolean rcaWorked = processManager.rca(changes, measurementMode);
         if (!rcaWorked) {
            run.setResult(Result.FAILURE);
            return;
         }
         processManager.visualizeRCAResults(run, changes);
      }
   }

   private PeassProcessConfiguration buildConfiguration(final FilePath workspace, final EnvVars env, final TaskListener listener, final Pattern pattern)
         throws IOException, InterruptedException {
      final MeasurementConfig configWithRealGitVersions = generateMeasurementConfig(workspace, listener);

      EnvironmentVariables peassEnv = new EnvironmentVariables(properties);
      for (Map.Entry<String, String> entry : env.entrySet()) {
         peassEnv.getEnvironmentVariables().put(entry.getKey(), entry.getValue());
      }

      DependencyConfig dependencyConfig = new DependencyConfig(1, false, true, generateCoverageSelection);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(updateSnapshotDependencies, configWithRealGitVersions, dependencyConfig,
            peassEnv,
            displayRTSLogs, displayLogs, displayRCALogs, pattern);
      return peassConfig;
   }

   private MeasurementConfig generateMeasurementConfig(final FilePath workspace, final TaskListener listener)
         throws IOException, InterruptedException {
      final MeasurementConfig measurementConfig = getMeasurementConfig();
      listener.getLogger().println("Starting RemoteVersionReader");
      final RemoteVersionReader remoteVersionReader = new RemoteVersionReader(measurementConfig, listener);
      final MeasurementConfig configWithRealGitVersions = workspace.act(remoteVersionReader);
      listener.getLogger()
            .println("Read version: " + configWithRealGitVersions.getExecutionConfig().getVersion() + " " + configWithRealGitVersions.getExecutionConfig().getVersionOld());
      return configWithRealGitVersions;
   }

   private void printRunMetadata(final Run<?, ?> run, final FilePath workspace, final TaskListener listener, final File localWorkspace) {
      listener.getLogger().println("Current Job: " + getJobName(run));
      listener.getLogger().println("Local workspace " + workspace.toString() + " Run dir: " + run.getRootDir() + " Local workspace: " + localWorkspace);
      listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
      listener.getLogger().println("measureJMH: " + measureJMH);
      listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);
      listener.getLogger().println("Excludes: " + excludes);
      listener.getLogger().println("Strategy: " + measurementMode + " Source Instrumentation: " + useSourceInstrumentation + " Aggregation: " + useAggregation);
      listener.getLogger().println("Create default constructor: " + createDefaultConstructor);
      listener.getLogger().println("Fail on error in RTS: " + failOnRtsError);
      listener.getLogger().println("Redirect subprocess output to file: " + redirectSubprocessOutputToFile);
   }

   private String getJobName(final Run<?, ?> run) {
      return run.getParent().getFullDisplayName();
   }

   public MeasurementConfig getMeasurementConfig() throws JsonParseException, JsonMappingException, IOException {
      if (significanceLevel == 0.0) {
         significanceLevel = 0.01;
      }
      final MeasurementConfig config = new MeasurementConfig(VMs);
      config.getExecutionConfig().setTimeout(timeout * 60l * 1000);
      config.getExecutionConfig().setKiekerWaitTime(kiekerWaitTime);
      config.getKiekerConfig().setKiekerQueueSize(kiekerQueueSize);
      config.getStatisticsConfig().setType1error(significanceLevel);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      config.setEarlyStop(false);
      config.getExecutionConfig().setCreateDefaultConstructor(createDefaultConstructor);
      config.getExecutionConfig().setExecuteBeforeClassInMeasurement(executeBeforeClassInMeasurement);
      config.getExecutionConfig().setOnlyMeasureWorkload(onlyMeasureWorkload);
      if (onlyMeasureWorkload && repetitions != 1) {
         throw new RuntimeException("If onlyMeasureWorkload is set, repetitions should be 1, but are " + repetitions);
      }
      config.getExecutionConfig().setRedirectToNull(redirectToNull);
      config.setShowStart(showStart);
      config.getExecutionConfig().setRemoveSnapshots(removeSnapshots);
      config.getExecutionConfig().setExcludeLog4j(excludeLog4j);
      if (executeParallel) {
         System.out.println("Measuring parallel");
         config.setMeasurementStrategy(MeasurementStrategy.PARALLEL);
      } else {
         System.out.println("executeparallel is false");
      }
      if (measureJMH) {
         config.getExecutionConfig().setTestTransformer("de.dagere.peass.dependency.jmh.JmhTestTransformer");
         config.getExecutionConfig().setTestExecutor("de.dagere.peass.dependency.jmh.JmhTestExecutor");
      }
      if (useSourceInstrumentation) {
         config.getKiekerConfig().setUseSourceInstrumentation(true);
         config.getKiekerConfig().setUseSelectiveInstrumentation(true);
         config.getKiekerConfig().setUseCircularQueue(true);
         if (useAggregation) {
            config.getKiekerConfig().setUseAggregation(true);
            config.getKiekerConfig().setRecord(AllowedKiekerRecord.DURATION);
         }
      }
      if (!"".equals(excludeForTracing)) {
         LinkedHashSet<String> excludeSet = IncludeExcludeParser.getStringSet(excludeForTracing);
         config.getKiekerConfig().setExcludeForTracing(excludeSet);
      }

      if (useAggregation && !useSourceInstrumentation) {
         throw new RuntimeException("Aggregation may only be used with source instrumentation currently.");
      }

      if (versionDiff <= 0) {
         throw new RuntimeException("The version difference should be at least 1, but was " + versionDiff);
      }
      if (nightlyBuild && versionDiff != 1) {
         throw new RuntimeException("If nightly build is set, do not set versionDiff! nightlyBuild will automatically select the last tested version.");
      }
      config.getKiekerConfig().setTraceSizeInMb(traceSizeInMb);

      config.getExecutionConfig().setVersion("HEAD");
      final String oldVersion = getOldVersion();
      config.getExecutionConfig().setVersionOld(oldVersion);

      config.getExecutionConfig().setIncludes(IncludeExcludeParser.getStringList(includes));
      config.getExecutionConfig().setExcludes(IncludeExcludeParser.getStringList(excludes));

      config.getExecutionConfig().setUseAlternativeBuildfile(useAlternativeBuildfile);
      config.getExecutionConfig().setRedirectSubprocessOutputToFile(redirectSubprocessOutputToFile);

      config.getExecutionConfig().setTestTransformer(testTransformer);
      config.getExecutionConfig().setTestExecutor(testExecutor);

      if (clazzFolders != null && !"".equals(clazzFolders.trim())) {
         List<String> pathes = ExecutionConfig.buildFolderList(clazzFolders);
         List<String> clazzFolders2 = config.getExecutionConfig().getClazzFolders();
         clazzFolders2.clear();
         clazzFolders2.addAll(pathes);
      }

      if (testClazzFolders != null && !"".equals(testClazzFolders.trim())) {
         List<String> testPathes = ExecutionConfig.buildFolderList(testClazzFolders);
         List<String> testClazzFolders2 = config.getExecutionConfig().getTestClazzFolders();
         testClazzFolders2.clear();
         testClazzFolders2.addAll(testPathes);
      }

      if (testGoal != null && !"".equals(testGoal)) {
         config.getExecutionConfig().setTestGoal(testGoal);
      }

      if (pl != null && !"".equals(pl)) {
         config.getExecutionConfig().setPl(pl);
      }

      config.getKiekerConfig().setOnlyOneCallRecording(onlyOneCallRecording);

      System.out.println("Building, iterations: " + iterations + " test goal: " + testGoal);
      return config;
   }

   private String getOldVersion() throws IOException, JsonParseException, JsonMappingException {
      final String oldVersion;
      if (nightlyBuild) {
         oldVersion = null;
      } else {
         oldVersion = "HEAD~" + versionDiff;
      }
      return oldVersion;
   }

   @Override
   public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.BUILD;
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

   public int getKiekerWaitTime() {
      return kiekerWaitTime;
   }

   @DataBoundSetter
   public void setKiekerWaitTime(final int kiekerWaitTime) {
      this.kiekerWaitTime = kiekerWaitTime;
   }

   public double getSignificanceLevel() {
      return significanceLevel;
   }

   @DataBoundSetter
   public void setSignificanceLevel(final double significanceLevel) {
      this.significanceLevel = significanceLevel;
   }

   public boolean isNightlyBuild() {
      return nightlyBuild;
   }

   @DataBoundSetter
   public void setNightlyBuild(final boolean nightlyBuild) {
      this.nightlyBuild = nightlyBuild;
   }

   public int getVersionDiff() {
      return versionDiff;
   }

   @DataBoundSetter
   public void setVersionDiff(final int versionDiff) {
      this.versionDiff = versionDiff;
   }

   public int getTraceSizeInMb() {
      return traceSizeInMb;
   }

   @DataBoundSetter
   public void setTraceSizeInMb(final int traceSizeInMb) {
      this.traceSizeInMb = traceSizeInMb;
   }

   public boolean isDisplayRTSLogs() {
      return displayRTSLogs;
   }

   @DataBoundSetter
   public void setDisplayRTSLogs(final boolean displayRTSLogs) {
      this.displayRTSLogs = displayRTSLogs;
   }

   public boolean isDisplayLogs() {
      return displayLogs;
   }

   @DataBoundSetter
   public void setDisplayLogs(final boolean displayLogs) {
      this.displayLogs = displayLogs;
   }

   public boolean isDisplayRCALogs() {
      return displayRCALogs;
   }

   @DataBoundSetter
   public void setDisplayRCALogs(final boolean displayRCALogs) {
      this.displayRCALogs = displayRCALogs;
   }

   public boolean isRedirectSubprocessOutputToFile() {
      return redirectSubprocessOutputToFile;
   }

   @DataBoundSetter
   public void setGenerateCoverageSelection(final boolean generateCoverageSelection) {
      this.generateCoverageSelection = generateCoverageSelection;
   }

   public boolean isGenerateCoverageSelection() {
      return generateCoverageSelection;
   }

   @DataBoundSetter
   public void setRedirectSubprocessOutputToFile(final boolean redirectSubprocessOutputToFile) {
      this.redirectSubprocessOutputToFile = redirectSubprocessOutputToFile;
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

   public String getExcludes() {
      return excludes;
   }

   @DataBoundSetter
   public void setExcludes(final String excludes) {
      this.excludes = excludes;
   }

   public boolean isExecuteRCA() {
      return executeRCA;
   }

   @DataBoundSetter
   public void setProperties(final String properties) {
      this.properties = properties;
   }

   public String getProperties() {
      return properties;
   }

   public String getTestGoal() {
      return testGoal;
   }

   @DataBoundSetter
   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public String getPl() {
      return pl;
   }

   @DataBoundSetter
   public void setPl(final String pl) {
      this.pl = pl;
   }

   @DataBoundSetter
   public void setExecuteRCA(final boolean executeRCA) {
      this.executeRCA = executeRCA;
   }

   public boolean isExecuteBeforeClassInMeasurement() {
      return executeBeforeClassInMeasurement;
   }

   @DataBoundSetter
   public void setExecuteBeforeClassInMeasurement(final boolean executeBeforeClassInMeasurement) {
      this.executeBeforeClassInMeasurement = executeBeforeClassInMeasurement;
   }

   public boolean isOnlyMeasureWorkload() {
      return onlyMeasureWorkload;
   }

   @DataBoundSetter
   public void setOnlyMeasureWorkload(final boolean onlyMeasureWorkload) {
      this.onlyMeasureWorkload = onlyMeasureWorkload;
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

   public boolean isUseAggregation() {
      return useAggregation;
   }

   @DataBoundSetter
   public void setUseAggregation(final boolean useAggregation) {
      this.useAggregation = useAggregation;
   }

   public boolean isExecuteParallel() {
      return executeParallel;
   }

   @DataBoundSetter
   public void setExecuteParallel(final boolean executeParallel) {
      this.executeParallel = executeParallel;
   }

   public boolean isCreateDefaultConstructor() {
      return createDefaultConstructor;
   }

   @DataBoundSetter
   public void setCreateDefaultConstructor(final boolean createDefaultConstructor) {
      this.createDefaultConstructor = createDefaultConstructor;
   }

   public boolean isUpdateSnapshotDependencies() {
      return updateSnapshotDependencies;
   }

   @DataBoundSetter
   public void setUpdateSnapshotDependencies(final boolean updateSnapshotDependencies) {
      this.updateSnapshotDependencies = updateSnapshotDependencies;
   }

   public boolean isRemoveSnapshots() {
      return removeSnapshots;
   }

   @DataBoundSetter
   public void setRemoveSnapshots(final boolean removeSnapshots) {
      this.removeSnapshots = removeSnapshots;
   }

   public boolean isUseAlternativeBuildfile() {
      return useAlternativeBuildfile;
   }

   @DataBoundSetter
   public void setUseAlternativeBuildfile(final boolean useAlternativeBuildfile) {
      this.useAlternativeBuildfile = useAlternativeBuildfile;
   }

   public boolean isExcludeLog4j() {
      return excludeLog4j;
   }

   @DataBoundSetter
   public void setExcludeLog4j(final boolean excludeLog4j) {
      this.excludeLog4j = excludeLog4j;
   }

   public boolean isRedirectToNull() {
      return redirectToNull;
   }

   @DataBoundSetter
   public void setRedirectToNull(final boolean redirectToNull) {
      this.redirectToNull = redirectToNull;
   }

   public boolean isShowStart() {
      return showStart;
   }

   @DataBoundSetter
   public void setShowStart(final boolean showStart) {
      this.showStart = showStart;
   }

   public boolean isMeasureJMH() {
      return measureJMH;
   }

   @DataBoundSetter
   public void setMeasureJMH(final boolean measureJMH) {
      this.measureJMH = measureJMH;
   }

   public String getTestExecutor() {
      return testExecutor;
   }

   @DataBoundSetter
   public void setTestExecutor(final String testExecutor) {
      this.testExecutor = testExecutor;
   }

   public String getTestTransformer() {
      return testTransformer;
   }

   @DataBoundSetter
   public void setTestTransformer(final String testTransformer) {
      this.testTransformer = testTransformer;
   }

   public String getClazzFolders() {
      return clazzFolders;
   }

   @DataBoundSetter
   public void setClazzFolders(final String clazzFolders) {
      this.clazzFolders = clazzFolders;
   }

   public String getTestClazzFolders() {
      return testClazzFolders;
   }

   @DataBoundSetter
   public void setTestClazzFolders(final String testClazzFolders) {
      this.testClazzFolders = testClazzFolders;
   }

   public boolean isFailOnRtsError() {
      return failOnRtsError;
   }

   @DataBoundSetter
   public void setFailOnRtsError(final boolean failOnRtsError) {
      this.failOnRtsError = failOnRtsError;
   }

   public long getKiekerQueueSize() {
      return kiekerQueueSize;
   }

   @DataBoundSetter
   public void setKiekerQueueSize(final long kiekerQueueSize) {
      this.kiekerQueueSize = kiekerQueueSize;
   }

   public boolean isOnlyOneCallRecording() {
      return onlyOneCallRecording;
   }

   @DataBoundSetter
   public void setOnlyOneCallRecording(final boolean onlyOneCallRecording) {
      this.onlyOneCallRecording = onlyOneCallRecording;
   }

   public String getExcludeForTracing() {
      return excludeForTracing;
   }

   @DataBoundSetter
   public void setExcludeForTracing(final String excludeForTracing) {
      this.excludeForTracing = excludeForTracing;
   }

   public String getCredentialsId() {
      return credentialsId;
   }

   @DataBoundSetter
   public void setCredentialsId(final String credentialsId) {
      this.credentialsId = credentialsId;
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

      public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
            @QueryParameter final String url,
            @QueryParameter final String credentialsId) {
         if (project == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) ||
               project != null && !project.hasPermission(Item.EXTENDED_READ)) {
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
         }
         if (project == null) {
            /*
             * Construct a fake project, suppress the deprecation warning because the replacement for the deprecated API isn't accessible in this context.
             */
            @SuppressWarnings("deprecation")
            Item fakeProject = new FreeStyleProject(Jenkins.get(), "fake-" + UUID.randomUUID().toString());
            project = fakeProject;
         }
         return new StandardListBoxModel()
               .includeEmptyValue()
               .includeMatchingAs(
                     project instanceof Queue.Task
                           ? Tasks.getAuthenticationOf((Queue.Task) project)
                           : ACL.SYSTEM,
                     project,
                     StandardUsernamePasswordCredentials.class,
                     new LinkedList<>(),
                     CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
               .includeCurrentValue(credentialsId);
      }

      public FormValidation doCheckCredentialsId(@AncestorInPath final Item project,
            @QueryParameter String url,
            @QueryParameter String value) {
         if (project == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) ||
               project != null && !project.hasPermission(Item.EXTENDED_READ)) {
            return FormValidation.ok();
         }

         value = Util.fixEmptyAndTrim(value);
         if (value == null) {
            return FormValidation.ok();
         }

         url = Util.fixEmptyAndTrim(url);
         if (url == null)
         // not set, can't check
         {
            return FormValidation.ok();
         }

         if (url.indexOf('$') >= 0)
         // set by variable, can't check
         {
            return FormValidation.ok();
         }
         for (ListBoxModel.Option o : CredentialsProvider
               .listCredentials(StandardUsernameCredentials.class, project, project instanceof Queue.Task
                     ? Tasks.getAuthenticationOf((Queue.Task) project)
                     : ACL.SYSTEM,
                     new LinkedList<>(),
                     CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))) {
            if (StringUtils.equals(value, o.value)) {
               // TODO check if this type of credential is acceptable to the Git client or does it merit warning
               // NOTE: we would need to actually lookup the credential to do the check, which may require
               // fetching the actual credential instance from a remote credentials store. Perhaps this is
               // not required
               return FormValidation.ok();
            }
         }
         // no credentials available, can't check
         return FormValidation.warning("Cannot find any credentials with id " + value);
      }

      public ListBoxModel doFillMeasurementModeItems(@QueryParameter final String measurementMode) {
         ListBoxModel model = new ListBoxModel();
         model.add(new Option("Complete", "COMPLETE", "COMPLETE".equals(measurementMode)));
         model.add(new Option("Levelwise", "LEVELWISE", "LEVELWISE".equals(measurementMode)));
         model.add(new Option("Until Source Change", "UNTIL_SOURCE_CHANGE", "UNTIL_SOURCE_CHANGE".equals(measurementMode)));
         model.add(new Option("Until Structure Change (NOT IMPLEMENTED)", "UNTIL_STRUCTURE_CHANGE", "UNTIL_STRUCTURE_CHANGE".equals(measurementMode)));
         model.add(new Option("Constant Levels(NOT IMPLEMENTED)", "CONSTANT_LEVELS", "CONSTANT_LEVELS".equals(measurementMode)));
         return model;
      }
   }
}
