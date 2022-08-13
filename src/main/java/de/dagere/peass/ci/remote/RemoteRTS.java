package de.dagere.peass.ci.remote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.ContinuousExecutor;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.RTSResult;
import de.dagere.peass.ci.process.JenkinsLogRedirector;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class RemoteRTS implements FileCallable<RTSResult> {
   private static final long serialVersionUID = -837869375735980083L;

   private static final Logger LOG = LogManager.getLogger(RemoteRTS.class);

   private final PeassProcessConfiguration peassConfig;

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
         String commitOld = executor.getCommitOld();
         try {
            RTSResult tests = executor.executeRTS();
            return tests;
         } catch (Throwable e) {
            printErrorInformation(workspaceFolder, e);
            RTSResult rtsResult = new RTSResult(null, false);
            rtsResult.setVersionOld(commitOld);
            return rtsResult;
         }
      } catch (Throwable e) {
         printErrorInformation(workspaceFolder, e);
         return null;
      }
   }

   private void printErrorInformation(final File workspaceFolder, final Throwable e) throws FileNotFoundException, UnsupportedEncodingException {
      File test = new File(workspaceFolder, "error.txt"); // Workaround, since error redirection on Jenkins agents currently does not work
      PrintStream writer = new PrintStream(test, "UTF-8");
      e.printStackTrace(writer);
      writer.flush();
      listener.getLogger().println("Exception thrown");
      e.printStackTrace(listener.getLogger());
      e.printStackTrace();
   }
}
