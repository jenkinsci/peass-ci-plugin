package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.io.FileMatchers;
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
   private File rtsLogFolder, measurementLogFolder;

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

      MatcherAssert.assertThat(rtsLogFolder, Matchers.not(FileMatchers.anExistingDirectory()));
      MatcherAssert.assertThat(measurementLogFolder, Matchers.not(FileMatchers.anExistingDirectory()));
   }

   private void createDummyData(final FreeStyleProject project) throws IOException {
      File rootDir = new File(project.getRootDir(), MeasureVersionBuilder.PEASS_FOLDER_NAME);
      rootDir.mkdirs();

      initializeFakeGitFolders(project, rootDir);

      dependencyFile = new File(rootDir, ResultsFolders.STATIC_SELECTION_PREFIX + project.getName() + ".json");
      Files.touch(dependencyFile);

      initializeLogFolders(project, rootDir);

      trendFile = new File(rootDir, "trend.json");
      Files.touch(trendFile);

      visualizationFolder = new File(rootDir, "visualization");
      visualizationFolder.mkdir();
   }

   private void initializeLogFolders(final FreeStyleProject project, File rootDir) throws IOException {
      File peassFolder = new File(rootDir, project.getName() + "_peass/");
      rtsLogFolder = new File(peassFolder, "logs/dependencyLogs");
      rtsLogFolder.mkdirs();
      Files.touch(new File(rtsLogFolder, "myLog.txt"));

      measurementLogFolder = new File(peassFolder, "logs/measureLogs");
      measurementLogFolder.mkdirs();
      Files.touch(new File(measurementLogFolder, "myLog.txt"));
   }

   private void initializeFakeGitFolders(final FreeStyleProject project, File rootDir) throws IOException {
      new File(rootDir, project.getName()).mkdir();
      Files.touch(new File(rootDir, project.getName() + "/.git"));

      File projectGitFile = new File(project.getRootDir(), "../../workspace/" + project.getName() + "/.git");
      projectGitFile.getParentFile().mkdirs();
      Files.touch(projectGitFile);

      File copiedGitFile = new File(project.getRootDir(), "../../workspace/" + project.getName() + "_fullPeass/" + project.getName() + "/.git");
      copiedGitFile.getParentFile().mkdirs();
      Files.touch(copiedGitFile);
   }
}
