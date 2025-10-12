package de.dagere.peass.ci.clean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;

import org.hamcrest.Matchers;
import org.hamcrest.io.FileMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.common.io.Files;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.folders.ResultsFolders;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CleanBuilderTest {

   private File dependencyFile, trendFile, visualizationFolder;
   private File rtsLogFolder, measurementLogFolder;

   private JenkinsRule jenkins;

   @BeforeEach
   void setUp(JenkinsRule rule) {
      jenkins = rule;
   }

   @Test
   void testEmptyFolderCleaning() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      createDummyData(project);

      CleanBuilder builder = new CleanBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      jenkins.buildAndAssertStatus(Result.SUCCESS, project);

      checkAllDeleted();
   }

   private void checkAllDeleted() {
      assertFalse(dependencyFile.exists(), "Dependencyfile " + dependencyFile.getAbsolutePath() + " should not exist");
      assertFalse(trendFile.exists());
      assertFalse(visualizationFolder.exists());

      assertThat(rtsLogFolder, Matchers.not(FileMatchers.anExistingDirectory()));
      assertThat(measurementLogFolder, Matchers.not(FileMatchers.anExistingDirectory()));
   }

   private void createDummyData(final FreeStyleProject project) throws Exception {
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

   private void initializeLogFolders(final FreeStyleProject project, File rootDir) throws Exception {
      File peassFolder = new File(rootDir, project.getName() + "_peass/");
      rtsLogFolder = new File(peassFolder, "logs/dependencyLogs");
      rtsLogFolder.mkdirs();
      Files.touch(new File(rtsLogFolder, "myLog.txt"));

      measurementLogFolder = new File(peassFolder, "logs/measureLogs");
      measurementLogFolder.mkdirs();
      Files.touch(new File(measurementLogFolder, "myLog.txt"));
   }

   private void initializeFakeGitFolders(final FreeStyleProject project, File rootDir) throws Exception {
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
