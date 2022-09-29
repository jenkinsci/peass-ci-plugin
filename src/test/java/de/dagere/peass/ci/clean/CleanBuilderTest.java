package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.common.io.Files;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.folders.ResultsFolders;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class CleanBuilderTest {

   @Rule
   public JenkinsRule jenkins = new JenkinsRule();

   private File dependencyFile, trendFile, visualizationFolder;

   @Test
   public void testEmptyFolderCleaning() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      createDummyData(project);

      CleanBuilder builder = new CleanBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);

      checkAllDeleted();
   }

   private void checkAllDeleted() {
      Assert.assertFalse("Dependencyfile " + dependencyFile.getAbsolutePath() + " should not exist", dependencyFile.exists());
      Assert.assertFalse(trendFile.exists());
      Assert.assertFalse(visualizationFolder.exists());
   }

   private void createDummyData(final FreeStyleProject project) throws IOException {
      File rootDir = new File(project.getRootDir(), MeasureVersionBuilder.PEASS_FOLDER_NAME);
      rootDir.mkdirs();
      dependencyFile = new File(rootDir, ResultsFolders.STATIC_SELECTION_PREFIX + project.getName() + ".json");
      Files.touch(dependencyFile);

      trendFile = new File(rootDir, "trend.json");
      Files.touch(trendFile);

      visualizationFolder = new File(rootDir, "visualization");
      visualizationFolder.mkdir();
   }
}
