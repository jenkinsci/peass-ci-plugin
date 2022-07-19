package de.dagere.peass.ci.peassOverview;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import de.dagere.peass.ci.helper.IdHelper;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class PeassOverviewBuilder extends Builder implements SimpleBuildStep, Serializable {
   private static final long serialVersionUID = 8464953102259678145L;

   public static final String LAST_DAY = "LAST_DAY";
   public static final String LAST_WEEK = "LAST_WEEK";
   public static final String LAST_MONTH = "LAST_MONTH";
   public static final String ALL = "ALL";
   
   private List<Project> projects;
   private String timespan = LAST_DAY;
   private String changeClassifications = "TODO;function;optimization;update";
   private String unmeasuredClassifications = "TODO;remoteServerCall";

   @DataBoundConstructor
   public PeassOverviewBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final EnvVars env, final Launcher launcher, final TaskListener listener) throws IOException {
      listener.getLogger().println("Generating Peass Overview");

      ProjectDataCreator creator = new ProjectDataCreator(projects, timespan);
      Map<String, ProjectData> projectData = creator.generateAllProjectData(run, listener);

      PeassOverviewAction action = new PeassOverviewAction(IdHelper.getId(), projectData, changeClassifications, unmeasuredClassifications);
      run.addAction(action);
   }

   public List<Project> getProjects() {
      return projects;
   }

   @DataBoundSetter
   public void setProjects(List<Project> projects) {
      this.projects = projects;
   }

   public String getTimespan() {
      return timespan;
   }

   @DataBoundSetter
   public void setTimespan(String timespan) {
      this.timespan = timespan;
   }

   public String getChangeClassifications() {
      return changeClassifications;
   }

   @DataBoundSetter
   public void setChangeClassifications(String changeClassifications) {
      this.changeClassifications = changeClassifications;
   }

   public String getUnmeasuredClassifications() {
      return unmeasuredClassifications;
   }

   @DataBoundSetter
   public void setUnmeasuredClassifications(String unmeasuredClassifications) {
      this.unmeasuredClassifications = unmeasuredClassifications;
   }

   @Symbol("peassOverview")
   @Extension
   public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

      @Override
      public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
         return true;
      }

      @Override
      public String getDisplayName() {
         return "Peass Overview";
      }
   }
}
