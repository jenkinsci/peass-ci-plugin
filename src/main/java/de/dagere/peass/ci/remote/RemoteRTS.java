package de.dagere.peass.ci.remote;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.ContinuousExecutor;
import de.dagere.peass.ci.JenkinsLogRedirector;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class RemoteRTS implements FileCallable<RTSResult> {
   private static final long serialVersionUID = -837869375735980083L;

   private static final Logger LOG = LogManager.getLogger(RemoteRTS.class);
   
   private final PeassProcessConfiguration peassConfig;
   private String versionOld;

   private final TaskListener listener;

   public RemoteRTS(final PeassProcessConfiguration peassConfig, final TaskListener listener) {
      this.peassConfig = peassConfig;
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public RTSResult invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      try (final JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         LOG.info("Starting remote invocation, VMs: " + peassConfig.getMeasurementConfig().getVms());

         if (peassConfig.isUpdateSnapshotDependencies()) {
            /*
             * This is just a workaround until all dependencies are available in maven central repository.
             */
            new SnapshotDependencyChecker(peassConfig.getMeasurementConfig(), workspaceFolder, listener.getLogger()).checkKopemeAndKieker();
         }
         
         final ContinuousExecutor executor = new ContinuousExecutor(workspaceFolder, 
               peassConfig.getMeasurementConfig(), 
               peassConfig.getDependencyConfig(), 
               peassConfig.getEnvVars());
         versionOld = executor.getVersionOld();
         Set<TestCase> tests = executor.executeRTS();
         RTSResult result = new RTSResult(tests, versionOld);
         return result;
      } catch (Throwable e) {
         File test = new File(workspaceFolder, "error.txt"); // Workaround, since error redirection on Jenkins agents currently does not work
         PrintStream writer = new PrintStream(test, "UTF-8");
         e.printStackTrace(writer);
         writer.flush();
         listener.getLogger().println("Exception thrown");
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return null;
      }
   }
   
   public String getVersionOld() {
      return versionOld;
   }
}
