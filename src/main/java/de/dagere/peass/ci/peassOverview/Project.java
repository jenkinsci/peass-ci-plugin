package de.dagere.peass.ci.peassOverview;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class Project extends AbstractDescribableImpl<Project> implements Serializable {
   private static final long serialVersionUID = -3864564528382064925L;

   private String project = "../MY-PROJECT";

   @DataBoundConstructor
   public Project(String project) {
      this.project = project;
   }

   public String getProject() {
      return project;
   }

   @DataBoundSetter
   public void setProject(String project) {
      this.project = project;
   }
   
   public String getProjectName() {
      return project.substring(project.lastIndexOf('/') + 1);
   }

   @Extension
   public static class ToolSelectionDescriptor extends Descriptor<Project> {

      @Override
      public String getDisplayName() {
         return StringUtils.EMPTY;
      }
   }
}