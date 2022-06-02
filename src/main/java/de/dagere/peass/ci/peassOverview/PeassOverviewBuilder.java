package de.dagere.peass.ci.peassOverview;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.peassAnalysis.Messages;
import de.dagere.peass.ci.peassAnalysis.PeassAnalysisBuilder;
import de.dagere.peass.ci.peassAnalysis.PeassAnalysisAction;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
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

   private List<Project> projects;
   private String referencePoint;

   @DataBoundConstructor
   public PeassOverviewBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final EnvVars env, final Launcher launcher, final TaskListener listener) throws IOException {
      listener.getLogger().println("Generating Peass Overview");

      Map<String, ProjectData> projectData = generateAllProjectData(run, listener);

      PeassOverviewAction action = new PeassOverviewAction(IdHelper.getId(), projectData);
      run.addAction(action);
   }

   private Map<String, ProjectData> generateAllProjectData(final Run<?, ?> run, final TaskListener listener) throws IOException, StreamReadException, DatabindException {
      Map<String, ProjectData> projectData = new LinkedHashMap<>();
      for (Project project : projects) {
         String projectPath = project.getProject();
         String projectName = project.getProjectName();

         ProjectData currentProjectData = getProjectData(run, listener, projectPath, projectName);
         projectData.put(projectName, currentProjectData);

      }
      return projectData;
   }

   private ProjectData getProjectData(final Run<?, ?> run, final TaskListener listener, String projectPath, String projectName)
         throws IOException, StreamReadException, DatabindException {
      File projectWorkspace = new File(run.getRootDir(),
            ".." + File.separator + ".." + File.separator + projectPath + File.separator + MeasureVersionBuilder.PEASS_FOLDER_NAME);

      ResultsFolders resultsFolders = new ResultsFolders(projectWorkspace, projectName);

      StaticTestSelection selection = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);

      if (resultsFolders.getTraceTestSelectionFile().exists()) {
         ExecutionData data = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);

         PeassAnalysisBuilder.removeNotTraceSelectedTests(selection, data);
      }

      ProjectChanges projectChanges = new ProjectChanges();
      final File changeFile = resultsFolders.getChangeFile();
      if (changeFile.exists()) {
         projectChanges = Constants.OBJECTMAPPER.readValue(resultsFolders.getChangeFile(), ProjectChanges.class);
      } else {
         listener.getLogger().println(changeFile.getAbsolutePath() + " does not exist! If there are no Trace-based Selected Tests, that's ok.");
      }
      ProjectData currentProjectData = new ProjectData(selection, projectChanges);
      return currentProjectData;
   }

   public List<Project> getProjects() {
      return projects;
   }

   @DataBoundSetter
   public void setProjects(List<Project> projects) {
      this.projects = projects;
   }

   public String getReferencePoint() {
      return referencePoint;
   }

   @DataBoundSetter
   public void setReferencePoint(String referencePoint) {
      this.referencePoint = referencePoint;
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
         return Messages.PerformanceAnalysis_DescriptorImpl_DisplayName();
      }
   }
}
