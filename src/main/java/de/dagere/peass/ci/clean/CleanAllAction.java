package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.Messages;
import de.dagere.peass.utils.Constants;
import hudson.FilePath;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;

public class CleanAllAction implements Action {

   private Job<?, ?> project;

   public CleanAllAction(final Job<?, ?> project) {
      this.project = project;
   }

   public String clean() throws JsonProcessingException {
      CleaningResult result;
      if (project instanceof WorkflowJob || project instanceof Project) {
         try {
            File persistentWorkspace = new File(project.getRootDir(), MeasureVersionBuilder.PEASS_FOLDER_NAME);
            FileUtils.cleanDirectory(persistentWorkspace);

            if (project instanceof WorkflowJob) {
               WorkflowJob job = (WorkflowJob) project;
               result = tryCleaning(job);
            } else if (project instanceof FreeStyleProject) {
               FreeStyleProject job = (FreeStyleProject) project;
               result = tryCleaning(job);
            } else {
               result = new CleaningResult(CleaningResult.FAILURE_COLOR, "Full cleaning currently imposible, not implemented for job type: " + project.getClass());
            }
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            result = new CleaningResult(CleaningResult.FAILURE_COLOR,  "Some error appeared during cleanup, please check Jenkins server logs");
         }
      } else {
         result = new CleaningResult(CleaningResult.FAILURE_COLOR,  "Unexpected project type");
      }
      return Constants.OBJECTMAPPER.writeValueAsString(result);
   }

   private CleaningResult tryCleaning(final TopLevelItem job) throws IOException, InterruptedException {
      Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
      if (jenkinsInstance != null) {
         FilePath path = jenkinsInstance.getWorkspaceFor(job);
         if (path == null) {
            return new CleaningResult(CleaningResult.FAILURE_COLOR, "There exists no workspace for job " + job.toString());
         }
         boolean cleaningWorked = path.act(new CleanAllCallable());
         if (cleaningWorked) {
            return new CleaningResult(CleaningResult.SUCCESS_COLOR, Messages.CleanAction_Success());
         } else {
            return new CleaningResult(CleaningResult.FAILURE_COLOR, "Some error appeared during cleanup, please check Jenkins server logs");
         }
      } else {
         return new CleaningResult(CleaningResult.FAILURE_COLOR, "Jenkins was not available");
      }
   }

   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return Messages.CleanAllAction_DisplayName();
   }

   @Override
   public String getUrlName() {
      return "cleanAll";
   }

}
