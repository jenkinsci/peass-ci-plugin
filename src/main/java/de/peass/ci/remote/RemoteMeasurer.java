package de.peass.ci.remote;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.RoleChecker;

import de.peass.ci.ContinuousExecutor;
import de.peass.ci.JenkinsLogRedirector;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.execution.EnvironmentVariables;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class RemoteMeasurer implements FileCallable<Boolean> {
   
   private static final Logger LOG = LogManager.getLogger(RemoteMeasurer.class);

   private static final long serialVersionUID = 5145199366806250594L;

   private final MeasurementConfiguration measurementConfig;
   private final EnvironmentVariables envVars;

   private final TaskListener listener;

   public RemoteMeasurer(final MeasurementConfiguration measurementConfig, final TaskListener listener, final EnvironmentVariables envVars) {
      this.measurementConfig = measurementConfig;
      this.listener = listener;
      this.envVars = envVars;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      try (final JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         LOG.info("Starting remote invocation, VMs: " + measurementConfig.getVms());
         // if (true) throw new RuntimeException("Finish with stupid exception");

         /*
          * This is just a workaround until all dependencies are available in maven central repository.
          */
         new SnapshotDependencyChecker(measurementConfig, workspaceFolder, listener.getLogger()).checkKopemeAndKieker();
         final ContinuousExecutor executor = new ContinuousExecutor(workspaceFolder, measurementConfig, 1, true, envVars);
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

}
