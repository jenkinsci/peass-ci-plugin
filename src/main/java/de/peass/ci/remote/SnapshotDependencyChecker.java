package de.peass.ci.remote;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SnapshotDependencyChecker {
   
   private static final Logger LOG = LogManager.getLogger(SnapshotDependencyChecker.class);
   private static final String seperator = File.separator;
   
   protected static void checkKopemeAndKieker(final File workspaceFolder) throws InterruptedException, IOException {

      final String home = System.getProperty("user.home");
      final String mavenRepo = home + seperator + ".m2" + seperator + "repository" + seperator;

      final File kopeme = new File(mavenRepo + "de" + seperator + "dagere" + seperator + "kopeme" + seperator + "kopeme-junit" + seperator + "0.14-SNAPSHOT");
      final File kieker = new File(mavenRepo + "net" + seperator + "kieker-monitoring" + seperator + "kieker" + seperator + "1.15-SNAPSHOT");

      if (!kopemeAndKiekerExist(kopeme, kieker)) {
         cloneAndinstallPeass(workspaceFolder);
      }

      if (!kopemeAndKiekerExist(kopeme, kieker)) {
         LOG.warn("Kopeme and/or Kieker dependencies could still not be found! Build will possibly fail!");
      }
   }

   private static boolean kopemeAndKiekerExist(final File kopeme, final File kieker) {
      return kopeme.exists() && kieker.exists();
   }

   private static void cloneAndinstallPeass(final File workspaceFolder) throws InterruptedException, IOException {
      LOG.warn("Kopeme and/or Kieker dependencies could not be found. Installing peass.");
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
      
      final ProcessBuilder builder = new ProcessBuilder(mavenWrapperName, "install", "-DskipTests")
            .directory(new File(workspaceFolder.getAbsolutePath() + seperator + ".." + seperator + "peass"))
            .redirectOutput(logFile);
      builder.start().waitFor();
   }

}
