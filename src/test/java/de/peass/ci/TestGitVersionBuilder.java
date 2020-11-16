package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestGitVersionBuilder {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testBuilding() throws InterruptedException, IOException {
      File tempFolder = folder.newFolder("testproject");

      GitProjectBuilder builder = new GitProjectBuilder(tempFolder, new File("src/test/resources/peass-demo/version1"));
      System.out.println(tempFolder.getAbsolutePath());

      testLogContains(tempFolder, "Initial Commit");

      builder.addVersion(new File("src/test/resources/peass-demo/version2"), "Slower Version");

      testLogContains(tempFolder, "Initial Commit", "Slower Version");
   }

   private void testLogContains(File tempFolder, final String... commitMessages) throws IOException {
      final Process logProcess = Runtime.getRuntime().exec("git log", new String[0], tempFolder);
      String output = IOUtils.toString(logProcess.getInputStream(), StandardCharsets.UTF_8);

      System.out.println("Output: " + output);
      MatcherAssert.assertThat(output, Matchers.containsString("commit"));

      for (String commitMessage : commitMessages) {
         MatcherAssert.assertThat(output, Matchers.containsString(commitMessage));
      }
   }
}
