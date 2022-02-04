package de.peass.ci.clean.callables;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import de.dagere.peass.ci.clean.CleanBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class CleanBuilderTest {
   
   @Rule
   public JenkinsRule jenkins = new JenkinsRule();
   
   @Test
   public void testEmptyFolderCleaning() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      CleanBuilder builder = new CleanBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);
   }
}
