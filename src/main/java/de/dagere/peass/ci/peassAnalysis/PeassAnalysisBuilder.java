package de.dagere.peass.ci.peassAnalysis;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.helper.IdHelper;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
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

      PeassAnalysisAction action = createAction(listener, localWorkspace, projectName);
      run.addAction(action);
   }

   public static PeassAnalysisAction createAction(final TaskListener listener, final File localWorkspace, String projectName) throws IOException, StreamReadException, DatabindException {
      ResultsFolders resultsFolders = new ResultsFolders(localWorkspace, projectName);

      StaticTestSelection selection = Constants.OBJECTMAPPER.readValue(resultsFolders.getStaticTestSelectionFile(), StaticTestSelection.class);

      if (resultsFolders.getTraceTestSelectionFile().exists()) {
         ExecutionData data = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);

         removeNotTraceSelectedTests(selection, data);
      }

      ProjectChanges projectChanges = new ProjectChanges();
      final File changeFile = resultsFolders.getChangeFile();
      if (changeFile.exists()) {
         projectChanges = Constants.OBJECTMAPPER.readValue(resultsFolders.getChangeFile(), ProjectChanges.class);
      } else {
         listener.getLogger().println(changeFile.getAbsolutePath() + " does not exist! If there are no Trace-based Selected Tests, that's ok.");
      }

      PeassAnalysisAction action = new PeassAnalysisAction(IdHelper.getId(), selection, projectChanges);
      return action;
   }

   public static void removeNotTraceSelectedTests(StaticTestSelection selection, ExecutionData data) {
      String newestVersion = selection.getNewestCommit();
      TestSet newestVersionTraceSelection = data.getVersions().get(newestVersion);
      if (newestVersionTraceSelection != null) {
         VersionStaticSelection versionStaticSelection = selection.getVersions().get(newestVersion);
         if (versionStaticSelection != null) {
            for (Map.Entry<ChangedEntity, TestSet> changedEntity : versionStaticSelection.getChangedClazzes().entrySet()) {
               Set<TestCase> tests = new HashSet<>(changedEntity.getValue().getTests());

               for (TestCase test : tests) {
                  if (!newestVersionTraceSelection.getTests().contains(test)) {
                     if (test.getMethod() != null) {
                        changedEntity.getValue().removeTest(test.onlyClazz(), test.getMethod());
                     } else {
                        changedEntity.getValue().removeTest(test);
                     }
                  }
               }
            }
         }
      }
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
