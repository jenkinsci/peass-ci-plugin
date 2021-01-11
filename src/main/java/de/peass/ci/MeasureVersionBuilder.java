package de.peass.ci;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.RCAStrategy;
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

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep {

   private int VMs;
   private int iterations;
   private int warmup;
   private int repetitions;
   private int timeout;
   private double significanceLevel;

   private int versionDiff;
   private boolean useGC;

   private String includes = "";
   private boolean executeRCA = true;
   private RCAStrategy measurementMode = RCAStrategy.LEVELWISE;

   @DataBoundConstructor
   public MeasureVersionBuilder(String test) {
      System.out.println("Initializing job: " + test);
   }

   @Override
   public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
      if (!workspace.exists()) {
         throw new RuntimeException("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().println("Executing on " + workspace.toString());
         listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
         listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);

         try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {

            ExecutionPerformer performer = new ExecutionPerformer(getConfig(), getIncludeList(), executeRCA, measurementMode);
            performer.performExecution(run, workspace);
         } catch (Throwable e) {
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

   public String getIncludes() {
      return includes;
   }

   @DataBoundSetter
   public void setIncludes(String includes) {
      this.includes = includes;
   }

   public boolean isExecuteRCA() {
      return executeRCA;
   }

   @DataBoundSetter
   public void setExecuteRCA(boolean executeRCA) {
      this.executeRCA = executeRCA;
   }

   public RCAStrategy getMeasurementMode() {
      return measurementMode;
   }

   @DataBoundSetter
   public void setMeasurementMode(RCAStrategy measurementMode) {
      this.measurementMode = measurementMode;
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
      
      public ListBoxModel doFillMeasurementModeItems(@QueryParameter String measurementMode) {
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
