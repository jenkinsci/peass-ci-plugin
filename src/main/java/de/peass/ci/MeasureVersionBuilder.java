package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fasterxml.jackson.databind.module.SimpleModule;

import de.peass.ci.persistence.TestcaseKeyDeserializer;
import de.peass.config.MeasurementConfiguration;
import de.peass.config.MeasurementStrategy;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.RCAStrategy;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
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
   private double significanceLevel;

   private int versionDiff = 1;
   private boolean useGC;

   private String includes = "";
   private boolean executeRCA = true;
   private RCAStrategy measurementMode = RCAStrategy.LEVELWISE;
   private boolean executeParallel = false;

   private boolean useSourceInstrumentation = true;
   private boolean useSampling = true;

   @DataBoundConstructor
   public MeasureVersionBuilder() {
      System.out.println("Initializing job");
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener) throws InterruptedException, IOException {
      if (!workspace.exists()) {
         throw new RuntimeException("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().println("Master workspace " + workspace.toString());
         listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
         listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);
         listener.getLogger().println("Strategy: " + measurementMode + " Source Instrumentation: " + useSourceInstrumentation + " Sampling: " + useSampling);

         try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
            File localRoot;
            if (run instanceof WorkflowRun) {
               WorkflowRun workflow = (WorkflowRun) run;
               localRoot = workflow.getRootDir();
            } else {
               localRoot = new File(workspace.toString());
            }
            final MeasurementConfiguration measurementConfig = getMeasurementConfig(localRoot);
            final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(null, true, true, 5.0, true, 0.01, false, true, measurementMode);
            ExecutionPerformer performer = new ExecutionPerformer(measurementConfig, executeRCA, causeSearcherConfig);
            performer.performExecution(run, localRoot);
         } catch (Throwable e) {
            e.printStackTrace(listener.getLogger());
            e.printStackTrace();
            run.setResult(Result.FAILURE);
         }
      }
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

   private MeasurementConfiguration getMeasurementConfig(final File workspaceFolder) {
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
      config.setVersion(GitUtils.getName("HEAD", workspaceFolder));
      config.setVersionOld(GitUtils.getName("HEAD~" + versionDiff, workspaceFolder));

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
