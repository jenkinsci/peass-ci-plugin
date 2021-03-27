package de.peass.ci.remote;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SnapshotDependencyChecker {

   private static final Logger LOG = LogManager.getLogger(SnapshotDependencyChecker.class);
   private static final String seperator = File.separator;

   protected static void checkKopemeAndKieker(final File workspaceFolder) throws InterruptedException, IOException {
      final File mavenRepo = getRepository();

      final File kopemeArtifactFolder = new File(mavenRepo, "de" + File.separator + "dagere" + File.separator + "kopeme" + File.separator +
            "kopeme-junit" + File.separator + "0.14-SNAPSHOT");
      final File kopeme = new File(kopemeArtifactFolder, "kopeme-junit-0.14-SNAPSHOT.jar");
      File kiekerArtifactFolder = new File(mavenRepo, "net" + File.separator + "kieker-monitoring" + File.separator +
            "kieker" + File.separator + "1.15-SNAPSHOT");
      final File kieker = new File(kiekerArtifactFolder, "kieker-1.15-SNAPSHOT-jar.jar");

      boolean snapshotDependenciesExist = kopemeAndKiekerExist(kopeme, kieker);
      LOG.info("Snapshot dependencies existing: {} Path: {}", snapshotDependenciesExist, kopeme.getAbsolutePath());
      if (!snapshotDependenciesExist) {
         cloneAndinstallPeass(workspaceFolder);
      }

      if (!kopemeAndKiekerExist(kopeme, kieker)) {
         LOG.warn("Kopeme and/or Kieker dependencies could still not be found! Build will possibly fail!");
      }
   }

   private static File getRepository() {
      String maven_home = System.getenv("HOME");
      LOG.debug("HOME: " + maven_home);

      final String mavenRepo;
      if (maven_home != null) {
         mavenRepo = maven_home + seperator + ".m2" + seperator + "repository" + seperator;
      } else {
         final String home = System.getProperty("user.home");
         mavenRepo = home + seperator + ".m2" + seperator + "repository" + seperator;
      }
      return new File(mavenRepo);
   }

   private static boolean kopemeAndKiekerExist(final File kopeme, final File kieker) {
      return kopeme.exists() && kieker.exists();
   }

   private static void cloneAndinstallPeass(final File workspaceFolder) throws InterruptedException, IOException {
      LOG.warn("Snapshot dependencies could not be found. Installing them.");
      clonePeass(workspaceFolder);
      installPeass(workspaceFolder);
   }

   private static void clonePeass(final File workspaceFolder) throws InterruptedException, IOException {
      final File logFile = new File(workspaceFolder, "clonePeassLog.txt");
      LOG.info("Cloning peass. Log goes to {}", logFile.getAbsolutePath());

      final ProcessBuilder builder = new ProcessBuilder("git", "clone", "--progress", "https://github.com/DaGeRe/peass")
            .directory(new File(workspaceFolder.getAbsolutePath() + seperator + ".."))
            .redirectError(logFile);
      builder.start().waitFor();
   }

   private static String getMavenCall() {
      String mvnCall;
      if (!System.getProperty("os.name").startsWith("Windows")) {
         mvnCall = "./mvnw";
      } else {
         mvnCall = "mvnw.cmd";
      }
      return mvnCall;
   }

   private static void installPeass(final File workspaceFolder) throws InterruptedException, IOException {
      final File logFile = new File(workspaceFolder, "installPeassLog.txt");
      LOG.info("Installing peass. Log goes to {}", logFile.getAbsolutePath());

      String mavenWrapperName = getMavenCall();

      File directory = new File(workspaceFolder.getAbsolutePath() + seperator + ".." + seperator + "peass");
      final ProcessBuilder builder = new ProcessBuilder(mavenWrapperName, "install", "-DskipTests")
            .directory(directory)
            .redirectOutput(logFile);
      builder.environment().put("MAVEN_CONFIG", "");
      // Somehow, using default set MAVEN_CONFIG lets install fail: https://github.com/aws/aws-codebuild-docker-images/issues/237
      LOG.debug("Full command: {}", builder.command());
      builder.start().waitFor();
   }

}
