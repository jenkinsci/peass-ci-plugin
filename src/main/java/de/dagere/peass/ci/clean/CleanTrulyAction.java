package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Project;
import jenkins.model.Jenkins;

public class CleanTrulyAction implements Action {

   private Job<?, ?> project;

   public CleanTrulyAction(final Job<?, ?> project) {
      this.project = project;
   }

   public String clean() {
      if (project instanceof WorkflowJob || project instanceof Project) {
         try {
            File persistentWorkspace = new File(project.getRootDir(), "peass-data");
            FileUtils.cleanDirectory(persistentWorkspace);

            if (project instanceof WorkflowJob) {
               WorkflowJob job = (WorkflowJob) project;
               Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
               if (jenkinsInstance != null) {
                  FilePath path = jenkinsInstance.getWorkspaceFor(job);
                  boolean cleaningWorked = path.act(new CleanCallable());
                  if (cleaningWorked) {
                     return "Cleaning succeeded";
                  } else {
                     return "Some error appeared during cleanup, please check Jenkins server logs";
                  }
               } else {
                  return "Jenkins was not available";
               }
            } else {
               return "Full cleaning currently imposible";
            }
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Some error appeared during cleanup, please check Jenkins server logs";
         }
      } else {
         return "Unexpected project type";
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
      return "cleanTruly";
   }

}
