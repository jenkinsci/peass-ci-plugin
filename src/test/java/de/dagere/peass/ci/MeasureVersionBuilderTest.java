package de.dagere.peass.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.WithTimeout;

import de.dagere.peass.ci.helper.GitProjectBuilder;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

@WithJenkins
class MeasureVersionBuilderTest {

   private static final int VMS = 2;

   private static final int ITERATIONS = 3;
   private static final int WARMUP = 1;
   private static final int REPETITIONS = 2;

   private JenkinsRule jenkins;

   @BeforeEach
   void setUp(JenkinsRule rule) {
      jenkins = rule;
   }
   @Test
   void testNoGitFailure() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      jenkins.buildAndAssertStatus(Result.FAILURE, project);
   }

   @Test
   // The tests sometimes fails in GH Actions or ci.jenkins.io because of timeout - therefore, the timeout is increased
   @WithTimeout(300)
   void testFullBuild() throws Exception {
      // Ignore this test on Jenkins infrastructure, since there is a problem with the file system
      Assumptions.assumeFalse(new File(".").getAbsolutePath().startsWith("C:\\Jenkins"));

      FreeStyleProject project = jenkins.createFreeStyleProject();
      initProjectFolder(project);

      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

      MeasurementOverviewAction action = build.getActions(MeasurementOverviewAction.class).get(0);

      assertEquals(ITERATIONS, action.getConfig().getIterations());
      assertEquals(VMS, action.getConfig().getVms());
      assertEquals(REPETITIONS, action.getConfig().getRepetitions());
      assertEquals(WARMUP, action.getConfig().getWarmup());
      assertEquals(0.05, action.getConfig().getStatisticsConfig().getType1error(), 0.01);
   }

   private void initProjectFolder(final FreeStyleProject project) throws Exception {
      jenkins.buildAndAssertStatus(Result.SUCCESS, project);

      FilePath path = project.getSomeWorkspace();

      File projectFolder = new File(path.getRemote());
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
