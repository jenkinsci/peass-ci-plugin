package de.peass.ci.remote;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.remoting.RoleChecker;

import de.peass.ci.ContinuousExecutor;
import de.peass.ci.JenkinsLogRedirector;
import de.peass.config.MeasurementConfiguration;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class RemoteMeasurer implements FileCallable<Boolean> {

   private static final long serialVersionUID = 5145199366806250594L;

   private final MeasurementConfiguration measurementConfig;

   private final TaskListener listener;

   public RemoteMeasurer(final MeasurementConfiguration measurementConfig, final TaskListener listener) {
      this.measurementConfig = measurementConfig;
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         System.out.println("Starting remote invocation, VMs: " + measurementConfig.getVms());
         // if (true) throw new RuntimeException("Finish with stupid exception");

         /*
          * This is just a workaround until all dependencies are available in maven central repository.
          */
         checkKopemeAndKieker(workspaceFolder);

         final ContinuousExecutor executor = new ContinuousExecutor(workspaceFolder, measurementConfig, 1, true);
         executor.execute();
         return true;
      } catch (Throwable e) {
         File test = new File(workspaceFolder, "error.txt"); // Workaround, since error redirection on Jenkins agents currently does not work
         PrintStream writer = new PrintStream(test, "UTF-8");
         e.printStackTrace(writer);
         writer.flush();
         listener.getLogger().println("Exception thrown");
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return false;
      }
   }

   private void checkKopemeAndKieker(final File workspaceFolder) throws InterruptedException, IOException {
      final File kopeme = new File("/home/ubuntu/.m2/repository/de/dagere/kopeme/kopeme-junit/0.14-SNAPSHOT");
      final File kieker = new File("/home/noname/.m2/repository/net/kieker-monitoring/kieker/1.15-SNAPSHOT");
      if(!kopeme.exists() || !kieker.exists()) {
         cloneAndinstallPeass(workspaceFolder);
      }
   }

   private void cloneAndinstallPeass(final File workspaceFolder) throws InterruptedException, IOException {
      System.out.println("Cloning peass");
      final ProcessBuilder builder = new ProcessBuilder("git", "clone", "https://github.com/DaGeRe/peass")
            .directory(new File(workspaceFolder.getAbsolutePath() + "/.."));
      builder.start().waitFor();

      builder.directory(new File(workspaceFolder.getAbsolutePath() + "/../peass"));
      builder.command("mvn", "install", "-DskipTests");
      builder.inheritIO().start().waitFor();
   }

}
