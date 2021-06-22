package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Project;
import hudson.remoting.VirtualChannel;
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
               FilePath path = Jenkins.getInstanceOrNull().getWorkspaceFor(job);
               boolean cleaningWorked = path.act(new FileCallable<Boolean>() {

                  @Override
                  public void checkRoles(final RoleChecker checker) throws SecurityException {
                  }

                  @Override
                  public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) {
                     try {
                        File folder = new File(potentialSlaveWorkspace.getParentFile(), potentialSlaveWorkspace.getName() + "_fullPeass");
                        System.out.println("Cleaning " + folder.getAbsolutePath());
                        FileUtils.cleanDirectory(folder);
                        return true;
                     } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                     }
                  }
               });
               if (cleaningWorked) {
                  return "Cleaning succeeded";
               } else {
                  return "Some error appeared during cleanup, please check Jenkins server logs";
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
