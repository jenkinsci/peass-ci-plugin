package de.dagere.peass.ci.remote;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;

public class SnapshotDependencyChecker {

   private static final Logger LOG = LogManager.getLogger(SnapshotDependencyChecker.class);
   private static final String seperator = File.separator;

   private final MeasurementConfig measurementConfig;
   private final File workspaceFolder;
   private final File kopemeFile, kiekerFile;
   private final PrintStream output;

   public SnapshotDependencyChecker(final MeasurementConfig measurementConfig, final File workspaceFolder, final PrintStream output) {
      this.measurementConfig = measurementConfig;
      this.workspaceFolder = workspaceFolder;
      this.output = output;

      final File mavenRepo = getRepository();
      final File kopemeArtifactFolder = new File(mavenRepo, "de" + File.separator + "dagere" + File.separator + "kopeme" + File.separator +
            "kopeme-junit" + File.separator + MavenPomUtil.KOPEME_VERSION);
      kopemeFile = new File(kopemeArtifactFolder, "kopeme-junit-" + MavenPomUtil.KOPEME_VERSION + ".jar");
      File kiekerArtifactFolder = new File(mavenRepo, "net" + File.separator + "kieker-monitoring" + File.separator +
            "kieker" + File.separator + MavenPomUtil.KIEKER_VERSION);
      kiekerFile = new File(kiekerArtifactFolder, "kieker-" + MavenPomUtil.KIEKER_VERSION + ".jar");
   }

   public void checkKopemeAndKieker() throws InterruptedException, IOException {
      if (MavenPomUtil.KIEKER_VERSION.contains("-SNAPSHOT") || MavenPomUtil.KOPEME_VERSION.contains("-SNAPSHOT")) {
         boolean snapshotDependenciesExist = kopemeAndKiekerExist();
         LOG.info("Snapshot dependencies existing: {} Path: {}", snapshotDependenciesExist, kopemeFile.getAbsolutePath());
         if (!snapshotDependenciesExist) {
            cloneAndinstallPeass();
         }

         if (!kopemeAndKiekerExist()) {
            LOG.warn("Kopeme and/or Kieker dependencies could still not be found! Build will possibly fail!");
         }
      } else {
         LOG.info("Kieker version {} and KoPeMe version {} where no snapshots, so no snapshot dependency checking necessary",
               MavenPomUtil.KIEKER_VERSION, MavenPomUtil.KOPEME_VERSION);
      }

   }

   private static File getRepository() {
      String maven_home = System.getProperty("user.home"); // formerly System.getenv("HOME"); - change back if problems occur
      LOG.debug("user.home: " + maven_home);

      final String mavenRepo;
      if (maven_home != null) {
         mavenRepo = maven_home + seperator + ".m2" + seperator + "repository" + seperator;
      } else {
         final String home = System.getProperty("user.home");
         mavenRepo = home + seperator + ".m2" + seperator + "repository" + seperator;
      }
      return new File(mavenRepo);
   }

   private boolean kopemeAndKiekerExist() {
      return kopemeFile.exists() && kiekerFile.exists();
   }

   private void cloneAndinstallPeass() throws InterruptedException, IOException {
      LOG.warn("Snapshot dependencies could not be found. Installing them.");
      clonePeass();
      installPeass();
   }

   private void clonePeass() throws InterruptedException, IOException {
      final File logFile = new File(workspaceFolder, "clonePeassLog.txt");
      LOG.info("Cloning peass.");

      File parentFolder = workspaceFolder.getParentFile();
      File peassFolder = new File(parentFolder, "peass");
      if (!peassFolder.exists()) {
         ProcessBuilder builder = new ProcessBuilder("git", "clone", "--branch", "develop", "--progress", "https://github.com/DaGeRe/peass")
               .directory(parentFolder);
         setRedirection(logFile, builder);
      } else {
         ProcessBuilder builder = new ProcessBuilder("git", "pull")
               .directory(peassFolder);
         setRedirection(logFile, builder);
      }
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

   private void installPeass() throws InterruptedException, IOException {
      final File logFile = new File(workspaceFolder, "installPeassLog.txt");
      LOG.info("Installing peass.");

      String mavenWrapperName = getMavenCall();

      File directory = new File(workspaceFolder.getParentFile(), "peass");
      ProcessBuilder builder = new ProcessBuilder(mavenWrapperName, "install", "-DskipTests")
            .directory(directory);
      // Somehow, using default set MAVEN_CONFIG lets install fail: https://github.com/aws/aws-codebuild-docker-images/issues/237
      builder.environment().put("MAVEN_CONFIG", "");
      setRedirection(logFile, builder);
   }

   private ProcessBuilder setRedirection(final File logFile, ProcessBuilder builder) throws InterruptedException, IOException {
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         LOG.debug("Log goes to {}", logFile.getAbsolutePath());
         builder = builder.redirectOutput(logFile)
               .redirectError(logFile);

         builder.start().waitFor();
      } else {
         LOG.debug("Not redirecting subprocess output to file, instead inheriting");
         builder.redirectErrorStream(true);

         Process process = builder.start();

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
               output.println(line);
            }

            process.waitFor();
         }
      }
      return builder;
   }

}
