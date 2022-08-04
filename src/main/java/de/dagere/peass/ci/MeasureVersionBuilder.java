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
import de.dagere.peass.ci.process.IncludeExcludeParser;
import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.ci.process.LocalPeassProcessManager;
import de.dagere.peass.ci.remote.RemoteVersionReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.config.StatisticalTests;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.deserializer.TestMethodCallKeyDeserializer;
import de.dagere.peass.dependency.analysis.data.deserializer.TestcaseKeyDeserializer;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
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

   private int VMs = MeasurementConfigurationMixin.DEFAULT_VMS;
   private int iterations = MeasurementConfigurationMixin.DEFAULT_ITERATIONS;
   private int warmup = MeasurementConfigurationMixin.DEFAULT_WARMUP;
   private int repetitions = MeasurementConfigurationMixin.DEFAULT_REPETITIONS;
   private long timeout = MeasurementConfigurationMixin.DEFAULT_TIMEOUT;

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
   private String includeByRule = "";
   private String excludeByRule = "";
   private String properties = "";
   private String testGoal = "test";
   private String pl = "";

   private RCAStrategy rcaStrategy = RCAStrategy.UNTIL_SOURCE_CHANGE;
   private StatisticalTests statisticalTest = StatisticalTests.T_TEST;

   private boolean executeBeforeClassInMeasurement = false;
   private boolean onlyMeasureWorkload = false;
   private boolean onlyOneCallRecording = false;

   private boolean updateSnapshotDependencies = false;
   private boolean removeSnapshots = false;
   private boolean useAlternativeBuildfile = false;
   private boolean excludeLog4jSlf4jImpl = false;
   private boolean excludeLog4jToSlf4j = false;

   private boolean useSourceInstrumentation = true;
   private boolean useAggregation = true;
   private boolean createDefaultConstructor = false;
   private boolean directlyMeasureKieker = false;

   private boolean redirectSubprocessOutputToFile = true;
   private boolean writeAsZip = true;

   private String testTransformer = "de.dagere.peass.testtransformation.JUnitTestTransformer";
   private String testExecutor = "default";

   private String clazzFolders = ExecutionConfigMixin.CLAZZ_FOLDERS_DEFAULT;
   private String testClazzFolders = ExecutionConfigMixin.TEST_FOLDERS_DEFAULT;

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
      String version = peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit();
      String versionOld = peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommitOld();
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
         throws IOException, InterruptedException, JsonParseException, JsonMappingException, JsonGenerationException, Exception {
      final LocalPeassProcessManager processManager = new LocalPeassProcessManager(peassConfig, workspace, localWorkspace, listener, run);

      AggregatedRTSResult tests = processManager.rts();
      listener.getLogger().println("Tests: " + tests);
      if (tests == null || !tests.getResult().isRunning()) {
         run.setResult(Result.FAILURE);
         return;
      }

      if (tests.isRtsAllError() || failOnRtsError && tests.isRtsAnyError()) {
         run.setResult(Result.FAILURE);
         return;
      }

      if (!failOnRtsError && tests.isRtsAnyError()) {
         run.setResult(Result.UNSTABLE);
      }

      processManager.visualizeRTSResults(run, tests.getLogSummary());

      if (tests.getResult().getTests().size() > 0) {
         measure(run, processManager, tests.getResult().getTests());
      } else {
         listener.getLogger().println("No tests selected; no measurement executed");
      }
   }

   private void measure(final Run<?, ?> run, final LocalPeassProcessManager processManager, final Set<TestMethodCall> tests)
         throws IOException, InterruptedException, JsonParseException, JsonMappingException, JsonGenerationException, Exception {
      boolean worked = processManager.measure(tests);
      if (!worked) {
         run.setResult(Result.FAILURE);
         return;
      }
      ProjectChanges changes = processManager.visualizeMeasurementResults(run);

      if (executeRCA) {
         final CauseSearcherConfig causeSearcherConfig = generateCauseSearchConfig();
         boolean rcaWorked = processManager.rca(changes, causeSearcherConfig);
         processManager.visualizeRCAResults(run, changes);
         if (!rcaWorked) {
            run.setResult(Result.FAILURE);
            return;
         }
         
      }
   }

   private PeassProcessConfiguration buildConfiguration(final FilePath workspace, final EnvVars env, final TaskListener listener, final Pattern pattern)
         throws IOException, InterruptedException {
      final MeasurementConfig configWithRealGitVersions = generateMeasurementConfig(workspace, listener);

      EnvironmentVariables peassEnv = new EnvironmentVariables(properties);
      for (Map.Entry<String, String> entry : env.entrySet()) {
         System.out.println("Adding environment: " + entry.getKey() + " " + entry.getValue());
         peassEnv.getEnvironmentVariables().put(entry.getKey(), entry.getValue());
      }

      TestSelectionConfig dependencyConfig = new TestSelectionConfig(1, false, true, generateCoverageSelection, writeAsZip);
      configWithRealGitVersions.getExecutionConfig().setGitCryptKey(peassEnv.getEnvironmentVariables().get("GIT_CRYPT_KEY"));
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(updateSnapshotDependencies, configWithRealGitVersions, dependencyConfig,
            peassEnv,
            displayRTSLogs, displayLogs, displayRCALogs, pattern);
      return peassConfig;
   }

   private CauseSearcherConfig generateCauseSearchConfig() {
      boolean ignoreEOIs = useAggregation;
      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(null, ignoreEOIs, 0.01, false, ignoreEOIs, rcaStrategy, 1);
      return causeSearcherConfig;
   }

   private MeasurementConfig generateMeasurementConfig(final FilePath workspace, final TaskListener listener)
         throws IOException, InterruptedException {
      final MeasurementConfig measurementConfig = getMeasurementConfig();
      listener.getLogger().println("Starting RemoteVersionReader");
      final RemoteVersionReader remoteVersionReader = new RemoteVersionReader(measurementConfig, listener);
      final MeasurementConfig configWithRealGitVersions = workspace.act(remoteVersionReader);
      listener.getLogger()
            .println("Read version: " + configWithRealGitVersions.getFixedCommitConfig().getCommit() + " " + configWithRealGitVersions.getFixedCommitConfig().getCommitOld());
      return configWithRealGitVersions;
   }

   private void printRunMetadata(final Run<?, ?> run, final FilePath workspace, final TaskListener listener, final File localWorkspace) {
      listener.getLogger().println("Current Job: " + getJobName(run));
      listener.getLogger().println("Local workspace " + workspace.toString() + " Run dir: " + run.getRootDir() + " Local workspace: " + localWorkspace);
      listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
      listener.getLogger().println("measureJMH: " + measureJMH);
      listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);
      listener.getLogger().println("Excludes: " + excludes);
      listener.getLogger().println("Strategy: " + rcaStrategy + " Source Instrumentation: " + useSourceInstrumentation + " Aggregation: " + useAggregation);
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
      
      config.getStatisticsConfig().setType1error(significanceLevel);
      config.getStatisticsConfig().setStatisticTest(statisticalTest);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      config.setEarlyStop(false);
      config.getExecutionConfig().setShowStart(showStart);
      config.setDirectlyMeasureKieker(directlyMeasureKieker);
      
      if (executeParallel) {
         System.out.println("Measuring parallel");
         config.setMeasurementStrategy(MeasurementStrategy.PARALLEL);
      } else {
         config.setMeasurementStrategy(MeasurementStrategy.SEQUENTIAL);
         System.out.println("executeparallel is false");
      }
      
      parameterizeKiekerConfig(config.getKiekerConfig());
      
      if (useAggregation && !useSourceInstrumentation) {
         throw new RuntimeException("Aggregation may only be used with source instrumentation currently.");
      }

      if (versionDiff <= 0) {
         throw new RuntimeException("The version difference should be at least 1, but was " + versionDiff);
      }
      if (nightlyBuild && versionDiff != 1) {
         throw new RuntimeException("If nightly build is set, do not set versionDiff! nightlyBuild will automatically select the last tested version.");
      }
      
      config.getFixedCommitConfig().setCommit("HEAD");
      final String oldVersion = getOldVersion();
      config.getFixedCommitConfig().setCommitOld(oldVersion);
      
      parameterizeExecutionConfig(config.getExecutionConfig());

      System.out.println("Building, iterations: " + iterations + " test goal: " + testGoal);
      return config;
   }

   private void parameterizeExecutionConfig(final ExecutionConfig executionConfig) throws IOException, JsonParseException, JsonMappingException {
      if (measureJMH) {
         executionConfig.setTestTransformer("de.dagere.peass.dependency.jmh.JmhTestTransformer");
         executionConfig.setTestExecutor("de.dagere.peass.dependency.jmh.JmhTestExecutor");
      }
      
      executionConfig.setTimeout(timeout * 60l * 1000);
      
      executionConfig.setExecuteBeforeClassInMeasurement(executeBeforeClassInMeasurement);
      executionConfig.setOnlyMeasureWorkload(onlyMeasureWorkload);
      if (onlyMeasureWorkload && repetitions != 1) {
         throw new RuntimeException("If onlyMeasureWorkload is set, repetitions should be 1, but are " + repetitions);
      }
      executionConfig.setRedirectToNull(redirectToNull);
      
      executionConfig.setRemoveSnapshots(removeSnapshots);
      executionConfig.setExcludeLog4jSlf4jImpl(excludeLog4jSlf4jImpl);
      executionConfig.setExcludeLog4jToSlf4j(excludeLog4jToSlf4j);

      executionConfig.setIncludes(IncludeExcludeParser.getStringList(includes));
      executionConfig.setExcludes(IncludeExcludeParser.getStringList(excludes));
      
      executionConfig.setIncludeByRule(IncludeExcludeParser.getStringListSimple(includeByRule));
      executionConfig.setExcludeByRule(IncludeExcludeParser.getStringListSimple(excludeByRule));
      
      executionConfig.setUseAlternativeBuildfile(useAlternativeBuildfile);
      executionConfig.setRedirectSubprocessOutputToFile(redirectSubprocessOutputToFile);

      executionConfig.setTestTransformer(testTransformer);
      executionConfig.setTestExecutor(testExecutor);
      
      if (clazzFolders != null && !"".equals(clazzFolders.trim())) {
         List<String> pathes = ExecutionConfig.buildFolderList(clazzFolders);
         List<String> clazzFolders2 = executionConfig.getClazzFolders();
         clazzFolders2.clear();
         clazzFolders2.addAll(pathes);
      }

      if (testClazzFolders != null && !"".equals(testClazzFolders.trim())) {
         List<String> testPathes = ExecutionConfig.buildFolderList(testClazzFolders);
         List<String> testClazzFolders2 = executionConfig.getTestClazzFolders();
         testClazzFolders2.clear();
         testClazzFolders2.addAll(testPathes);
      }

      if (testGoal != null && !"".equals(testGoal)) {
         executionConfig.setTestGoal(testGoal);
      }

      if (pl != null && !"".equals(pl)) {
         executionConfig.setPl(pl);
      }

      if (executionConfig.isExecuteBeforeClassInMeasurement() && executionConfig.isOnlyMeasureWorkload()) {
         throw new RuntimeException("executeBeforeClassInMeasurement may only be activated if onlyMeasureWorkload is deactivated!");
      }
   }

   private void parameterizeKiekerConfig(final KiekerConfig kiekerConfig) {
      kiekerConfig.setKiekerQueueSize(kiekerQueueSize);
      if (useSourceInstrumentation) {
         kiekerConfig.setUseSourceInstrumentation(true);
         kiekerConfig.setUseSelectiveInstrumentation(true);
         kiekerConfig.setUseCircularQueue(true);
      }
      if (useAggregation) {
         kiekerConfig.setUseAggregation(true);
         kiekerConfig.setRecord(AllowedKiekerRecord.DURATION);
      } else {
         kiekerConfig.setUseAggregation(false);
         kiekerConfig.setRecord(AllowedKiekerRecord.OPERATIONEXECUTION);
      }
      if (!"".equals(excludeForTracing)) {
         LinkedHashSet<String> excludeSet = IncludeExcludeParser.getStringSet(excludeForTracing);
         kiekerConfig.setExcludeForTracing(excludeSet);
      }
      kiekerConfig.setTraceSizeInMb(traceSizeInMb);
      kiekerConfig.setOnlyOneCallRecording(onlyOneCallRecording);
      kiekerConfig.setCreateDefaultConstructor(createDefaultConstructor);
      kiekerConfig.setKiekerWaitTime(kiekerWaitTime);
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

   public long getTimeout() {
      return timeout;
   }

   @DataBoundSetter
   public void setTimeout(final long timeout) {
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

   public String getIncludeByRule() {
      return includeByRule;
   }
   
   @DataBoundSetter
   public void setIncludeByRule(String includeByRule) {
      this.includeByRule = includeByRule;
   }
   
   public String getExcludeByRule() {
      return excludeByRule;
   }
   
   @DataBoundSetter
   public void setExcludeByRule(String excludeByRule) {
      this.excludeByRule = excludeByRule;
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
      return rcaStrategy;
   }

   @DataBoundSetter
   public void setMeasurementMode(final RCAStrategy measurementMode) {
      this.rcaStrategy = measurementMode;
   }

   public StatisticalTests getStatisticalTest() {
      return statisticalTest;
   }

   @DataBoundSetter
   public void setStatisticalTest(final StatisticalTests statisticalTest) {
      this.statisticalTest = statisticalTest;
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

   public boolean isExcludeLog4jSlf4jImpl() {
      return excludeLog4jSlf4jImpl;
   }
   
   @DataBoundSetter
   public void setExcludeLog4jSlf4jImpl(final boolean excludeLog4jSlf4jImpl) {
      this.excludeLog4jSlf4jImpl = excludeLog4jSlf4jImpl;
   }
   
   public boolean isExcludeLog4jToSlf4j() {
      return excludeLog4jToSlf4j;
   }
   
   @DataBoundSetter
   public void setExcludeLog4jToSlf4j(final boolean excludeLog4jToSlf4j) {
      this.excludeLog4jToSlf4j = excludeLog4jToSlf4j;
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
   
   public boolean isWriteAsZip() {
      return writeAsZip;
   }
   
   @DataBoundSetter
   public void setWriteAsZip(boolean writeAsZip) {
      this.writeAsZip = writeAsZip;
   }
   
   public boolean isDirectlyMeasureKieker() {
      return directlyMeasureKieker;
   }
   
   @DataBoundSetter
   public void setDirectlyMeasureKieker(boolean directlyMeasureKieker) {
      this.directlyMeasureKieker = directlyMeasureKieker;
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
         model.add(new Option("Constant Levels (NOT IMPLEMENTED)", "CONSTANT_LEVELS", "CONSTANT_LEVELS".equals(measurementMode)));
         return model;
      }

      public ListBoxModel doFillStatisticalTestItems(@QueryParameter final String statisticalTest) {
         ListBoxModel model = new ListBoxModel();
         model.add(new Option("T-Test", "T_TEST", "T_TEST".equals(statisticalTest)));
         model.add(new Option("Mann-Whitney-Test", "MANN_WHITNEY_TEST", "MANN_WHITNEY_TEST".equals(statisticalTest)));
         model.add(new Option("Confidence Interval Comparison", "CONFIDENCE_INTERVAL", "CONFIDENCE_INTERVAL".equals(statisticalTest)));
         return model;
      }
   }
}
