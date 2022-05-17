package de.dagere.peass.ci.peassAnalysis;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.helper.IdHelper;
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
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

public class PeassAnalysisBuilder extends Builder implements SimpleBuildStep, Serializable {
   private static final long serialVersionUID = 8464953102259678146L;

   @DataBoundConstructor
   public PeassAnalysisBuilder() {
   }

   @Override
   public void perform(final Run<?, ?> run, final FilePath workspace, final EnvVars env, final Launcher launcher, final TaskListener listener) throws IOException {
      listener.getLogger().println("Generating Peass Analysis");

      final File localWorkspace = new File(run.getRootDir(), ".." + File.separator + ".." + File.separator + MeasureVersionBuilder.PEASS_FOLDER_NAME).getCanonicalFile();
      String projectName = new File(workspace.getRemote()).getName();
      
      ResultsFolders resultsFolders = new ResultsFolders(localWorkspace, projectName);
      
      StaticTestSelection selection = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);
      
      ProjectChanges projectChanges = new ProjectChanges();
      final File changeFile = resultsFolders.getChangeFile();
      if (changeFile.exists()) {
         projectChanges = Constants.OBJECTMAPPER.readValue(resultsFolders.getChangeFile(), ProjectChanges.class);
      } else {
         listener.getLogger().println(changeFile.getAbsolutePath() + " does not exist! If there are no Trace-based Selected Tests, that's ok.");
      }

      PeassAnalysisAction action = new PeassAnalysisAction(IdHelper.getId(), selection, projectChanges);
      run.addAction(action);
   }
   
   @Symbol("peassAnalysis")
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
