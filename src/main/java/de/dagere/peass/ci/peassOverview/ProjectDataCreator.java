package de.dagere.peass.ci.peassOverview;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.peassAnalysis.PeassAnalysisBuilder;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ProjectDataCreator {

   private final List<Project> projects;
   private final String referencePoint;

   public ProjectDataCreator(List<Project> projects, String referencePoint) {
      this.projects = projects;
      this.referencePoint = referencePoint;
   }

   public Map<String, ProjectData> generateAllProjectData(final Run<?, ?> run, final TaskListener listener) throws IOException, StreamReadException, DatabindException {
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
      if (!projectWorkspace.exists()) {
         throw new RuntimeException("Expected folder " + projectWorkspace.getAbsolutePath() + " did not exist");
      }

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
      DateTime currentDate = new DateTime().withTimeAtStartOfDay();
      DateTime yesterday = currentDate.minusDays(1).withTimeAtStartOfDay();
      DateTime oneWeekBefore = currentDate.minusDays(7).withTimeAtStartOfDay();

      List<String> includedCommits = new LinkedList<>();
      List<LinkedHashMap<String, String>> commitMetadata = Constants.OBJECTMAPPER.readValue(resultsFolders.getCommitMetadataFile(), List.class);

      for (Map<String, String> commit : commitMetadata) {
         DateTime commitDate = getCommitDate(commit);

         if (referencePoint.equals(PeassOverviewBuilder.LAST_DAY)) {
            if (commitDate.isEqual(currentDate) || commitDate.isEqual(yesterday)) {
               includedCommits.add(commit.get("tag"));
            }
         } else if (referencePoint.equals(PeassOverviewBuilder.LAST_WEEK)) {
            if (commitDate.isEqual(currentDate) || (commitDate.isAfter(oneWeekBefore) && commitDate.isBefore(currentDate))) {
               includedCommits.add(commit.get("tag"));
            }
         }
      }

      return includedCommits;
   }

   private DateTime getCommitDate(Map<String, String> commit) {
      String jtdate = commit.get("date");
      String onlyDay = jtdate.substring(0, jtdate.indexOf(' '));
      DateTime commitDate = DATE_PARSER.parseDateTime(onlyDay);
      return commitDate;
   }
}
