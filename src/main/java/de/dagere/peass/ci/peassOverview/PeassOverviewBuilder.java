package de.dagere.peass.ci.peassOverview;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.Symbol;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.ci.peassAnalysis.Messages;
import de.dagere.peass.ci.peassAnalysis.PeassAnalysisBuilder;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitCommit;
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
   private String referencePoint = "LAST_DAY";

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

      List<String> includedCommits = findIncludedVersions(resultsFolders);

      StaticTestSelection selection = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);

      removeNotIncludedVersions(includedCommits, selection);

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

   private void removeNotIncludedVersions(List<String> includedCommits, StaticTestSelection selection) {
      Set<String> usedVersions = new HashSet<>(selection.getVersions().keySet());
      for (String version : usedVersions) {
         if (!includedCommits.contains(version)) {
            selection.getVersions().remove(version);
         }
      }
   }

   private static final DateTimeFormatter DATE_PARSER = ISODateTimeFormat.date();
   
   private List<String> findIncludedVersions(ResultsFolders resultsFolders) throws IOException, StreamReadException, DatabindException {
      DateTime currentDate = new DateTime();
      List<String> includedCommits = new LinkedList<>();
      List<LinkedHashMap<String, String>> commitMetadata = Constants.OBJECTMAPPER.readValue(resultsFolders.getCommitMetadataFile(), List.class);
      if (referencePoint.equals("LAST_DAY")) {
         for (Map<String, String> commit : commitMetadata) {
            
            String jtdate = commit.get("date");
            String onlyDay = jtdate.substring(0, jtdate.indexOf(' ') );
            
            DateTime dateTime = DATE_PARSER.parseDateTime(onlyDay);

            if (dateTime.getDayOfMonth() == currentDate.getDayOfMonth() || dateTime.getDayOfMonth() == currentDate.getDayOfMonth() - 1 
                  && dateTime.getMonthOfYear() == currentDate.getMonthOfYear()
                  && dateTime.getYear() == currentDate.getYear()) {
               includedCommits.add(commit.get("tag"));
            }
         }
      }
      return includedCommits;
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
