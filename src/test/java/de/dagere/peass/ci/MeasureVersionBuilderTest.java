package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithTimeout;

import de.dagere.peass.ci.helper.GitProjectBuilder;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class MeasureVersionBuilderTest {
   private static final int VMS = 2;
   
   private static final int ITERATIONS = 3;
   private static final int WARMUP = 1;
   private static final int REPETITIONS = 2;
   
   @Rule
   public JenkinsRule jenkins = new JenkinsRule();
   
   @Test
   public void testNoGitFailure() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
   }
   
   @Test
   // The tests sometimes fails in GH Actions or ci.jenkins.io because of timeout - therefore, the timeout is increased
   @WithTimeout(300)
   public void testFullBuild() throws Exception {
      // Ignore this test on Jenkins infrastructure, since there is a problem with the file system
      Assume.assumeFalse(new File(".").getAbsolutePath().startsWith("C:\\Jenkins"));
      
      FreeStyleProject project = jenkins.createFreeStyleProject();
      initProjectFolder(project);
      
      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

      MeasurementOverviewAction action = build.getActions(MeasurementOverviewAction.class).get(0);

      Assert.assertEquals(ITERATIONS, action.getConfig().getIterations());
      Assert.assertEquals(VMS, action.getConfig().getVms());
      Assert.assertEquals(REPETITIONS, action.getConfig().getRepetitions());
      Assert.assertEquals(WARMUP, action.getConfig().getWarmup());
      Assert.assertEquals(0.05, action.getConfig().getStatisticsConfig().getType1error(), 0.01);
   }

   private void initProjectFolder(final FreeStyleProject project) throws Exception, InterruptedException, IOException {
      jenkins.buildAndAssertStatus(Result.SUCCESS, project);
      
      FilePath path = project.getSomeWorkspace();
      
      File projectFolder = new File(path.toString());
      GitProjectBuilder gitbuilder = new GitProjectBuilder(projectFolder, new File(TestConstants.RESOURCE_FOLDER, "peass-demo/commit1"));
      gitbuilder.addCommit(new File(TestConstants.RESOURCE_FOLDER, "peass-demo/commit2"), "Slower Commit");
   }

   private MeasureVersionBuilder createSimpleBuilder() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIterations(ITERATIONS);
      builder.setVMs(VMS);
      builder.setRepetitions(REPETITIONS);
      builder.setWarmup(WARMUP);
      builder.setSignificanceLevel(0.05);
      builder.setExecuteRCA(false);
      builder.setRedirectSubprocessOutputToFile(false);
      builder.setShowStart(true);
      return builder;
   }
}
