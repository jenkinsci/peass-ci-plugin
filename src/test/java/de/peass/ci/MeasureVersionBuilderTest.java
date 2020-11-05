package de.peass.ci;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class MeasureVersionBuilderTest {
   @Rule
   public JenkinsRule jenkins = new JenkinsRule();

   @Test
   public void testConfiguration() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();
      MeasureVersionBuilder builder = new MeasureVersionBuilder("test");
      builder.setIterations(11);
      builder.setVMs(23);
      builder.setRepetitions(28);
      builder.setWarmup(17);
      builder.setSignificanceLevel(0.05);

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

      MeasureVersionAction action = build.getActions(MeasureVersionAction.class).get(0);

      Assert.assertEquals(11, action.getConfig().getIterations());
      Assert.assertEquals(23, action.getConfig().getVms());
      Assert.assertEquals(0.05, action.getConfig().getType1error(), 0.01);
   }
}
