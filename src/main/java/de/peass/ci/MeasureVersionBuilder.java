package de.peass.ci;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import de.peass.ContinuousExecutor;
import de.peass.dependency.execution.MeasurementConfiguration;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep {

   private int VMs;
   private String iterations;
   private int warmup;
   private int repetitions;
   private int timeout;
   private int significanceLevel;
   
   private int versionDiff;
   private boolean useGC;
   
   @DataBoundConstructor
   public MeasureVersionBuilder(String test) {
      System.out.println("Initializing" + test);
   }
   
   @Override
   public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

      System.out.println("Building, iterations: " + iterations);
      
      if (!workspace.exists()) {
         listener.getLogger().print("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().print("Executing on " + workspace.toString());
         try {
            ContinuousExecutor.main(new String[] { "-folder", workspace.toString() });
         } catch (Throwable e) {
            e.printStackTrace();
         }
      }

      /*
       * Now, we actually need to create an instance of this class when the build step is executed. We need to extend the perform method in the HelloWorldBuilder class to add an
       * instance of the action we created to the build that’s being run
       */
      final MeasurementConfiguration config = new MeasurementConfiguration(timeout, VMs, significanceLevel, 0.01);
      config.setIterations(Integer.parseInt(iterations));
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      run.addAction(new MeasureVersionAction(config));
   }
   
   

   public int getVMs() {
      return VMs;
   }

   @DataBoundSetter
   public void setVMs(int vMs) {
      VMs = vMs;
   }

   public String getIterations() {
      return iterations;
   }

   @DataBoundSetter
   public void setIterations(String iterations) {
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

   public int getSignificanceLevel() {
      return significanceLevel;
   }

   @DataBoundSetter
   public void setSignificanceLevel(int significanceLevel) {
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
