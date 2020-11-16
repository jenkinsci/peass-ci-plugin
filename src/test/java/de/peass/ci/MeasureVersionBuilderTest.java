package de.peass.ci;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import de.peass.vcs.GitUtils;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class MeasureVersionBuilderTest {
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
   public void testFullBuild() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();
      initProjectFolder(project);
      
      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

      MeasureVersionAction action = build.getActions(MeasureVersionAction.class).get(0);

      Assert.assertEquals(11, action.getConfig().getIterations());
      Assert.assertEquals(23, action.getConfig().getVms());
      Assert.assertEquals(0.05, action.getConfig().getType1error(), 0.01);
   }

   private void initProjectFolder(FreeStyleProject project) throws Exception, InterruptedException, IOException {
      jenkins.buildAndAssertStatus(Result.SUCCESS, project);
      
      FilePath path = project.getSomeWorkspace();
      
      File projectFolder = new File(path.toString());
      GitProjectBuilder gitbuilder = new GitProjectBuilder(projectFolder, new File("src/test/resources/peass-demo/version1"));
      gitbuilder.addVersion(new File("src/test/resources/peass-demo/version2"), "Slower Version");
   }

   private MeasureVersionBuilder createSimpleBuilder() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder("test");
      builder.setIterations(11);
      builder.setVMs(23);
      builder.setRepetitions(28);
      builder.setWarmup(17);
      builder.setSignificanceLevel(0.05);
      return builder;
   }
}
