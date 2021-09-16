package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

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

   public String clean() {
      if (project instanceof WorkflowJob || project instanceof Project) {
         try {
            File persistentWorkspace = new File(project.getRootDir(), "peass-data");
            FileUtils.cleanDirectory(persistentWorkspace);

            if (project instanceof WorkflowJob) {
               WorkflowJob job = (WorkflowJob) project;
               return tryCleaning(job);
            } else if (project instanceof FreeStyleProject) {
               FreeStyleProject job = (FreeStyleProject) project;
               return tryCleaning(job);
            } else {
               return "Full cleaning currently imposible, not implemented for job type: " + project.getClass();
            }
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Some error appeared during cleanup, please check Jenkins server logs";
         }
      } else {
         return "Unexpected project type";
      }
   }

   private String tryCleaning(final TopLevelItem job) throws IOException, InterruptedException {
      Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
      if (jenkinsInstance != null) {
         FilePath path = jenkinsInstance.getWorkspaceFor(job);
         if (path == null) {
            return "There exists no workspace for job " + job.toString();
         }
         boolean cleaningWorked = path.act(new CleanAllCallable());
         if (cleaningWorked) {
            return "Cleaning succeeded";
         } else {
            return "Some error appeared during cleanup, please check Jenkins server logs";
         }
      } else {
         return "Jenkins was not available";
      }
   }

   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return "Clean Peass-CI Cache";
   }

   @Override
   public String getUrlName() {
      return "cleanAll";
   }

}
