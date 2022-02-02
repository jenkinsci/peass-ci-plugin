package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.clean.callables.CleanMeasurementCallable;
import de.dagere.peass.ci.clean.callables.CleanRCACallable;
import de.dagere.peass.ci.clean.callables.CleanRTSCallable;
import hudson.EnvVars;
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
import jenkins.tasks.SimpleBuildStep;

public class CleanBuilder extends Builder implements SimpleBuildStep, Serializable {

   private static final long serialVersionUID = 9194518872489989179L;

   private boolean cleanRTS = true;
   private boolean cleanMeasurement = true;
   private boolean cleanRCA = true;

   @DataBoundConstructor
   public CleanBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final EnvVars env, final Launcher launcher, final TaskListener listener)
         throws InterruptedException, IOException {
      listener.getLogger().println("Cleaning");

      final File localWorkspace = new File(run.getRootDir(), ".." + File.separator + ".." + File.separator + MeasureVersionBuilder.PEASS_FOLDER_NAME).getCanonicalFile();
      String projectName = new File(workspace.getRemote()).getName();

      if (cleanRTS) {
         boolean worked = workspace.act(new CleanRTSCallable(listener));
         if (!worked) {
            run.setResult(Result.FAILURE);
            return;
         }
      } else {
         listener.getLogger().println("Regression Test Selection cleaning disabled");
      }
      if (cleanMeasurement) {
         boolean worked = workspace.act(new CleanMeasurementCallable(listener));
         if (!worked) {
            run.setResult(Result.FAILURE);
            return;
         }
      } else {
         listener.getLogger().println("Measurement cleaning disabled");
      }
      if (cleanRCA) {
         boolean worked = workspace.act(new CleanRCACallable(listener));
         if (!worked) {
            run.setResult(Result.FAILURE);
            return;
         }
         CleanRCACallable.cleanFolder(projectName, localWorkspace);
      } else {
         listener.getLogger().println("RCA cleaning disabled");
      }
   }

   public boolean isCleanRTS() {
      return cleanRTS;
   }

   @DataBoundSetter
   public void setCleanRTS(final boolean cleanRTS) {
      this.cleanRTS = cleanRTS;
   }

   public boolean isCleanMeasurement() {
      return cleanMeasurement;
   }

   @DataBoundSetter
   public void setCleanMeasurement(final boolean cleanMeasurement) {
      this.cleanMeasurement = cleanMeasurement;
   }

   public boolean isCleanRCA() {
      return cleanRCA;
   }

   @DataBoundSetter
   public void setCleanRCA(final boolean cleanRCA) {
      this.cleanRCA = cleanRCA;
   }

   @Symbol("cleanPerformanceMeasurement")
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
         return Messages.Clean_DescriptorImpl_DisplayName();
      }

   }

}
