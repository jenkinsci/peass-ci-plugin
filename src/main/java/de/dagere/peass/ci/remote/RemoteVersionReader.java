package de.dagere.peass.ci.remote;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.vcs.GitUtils;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class RemoteVersionReader implements FileCallable<MeasurementConfig> {

   private static final long serialVersionUID = -1266048917282327539L;

   private final MeasurementConfig measurementConfig;

   private final TaskListener listener;

   public RemoteVersionReader(final MeasurementConfig measurementConfig, final TaskListener listener) {
      this.measurementConfig = measurementConfig;
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public MeasurementConfig invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      try (JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         final String version = GitUtils.getName("HEAD", workspaceFolder);
         measurementConfig.getExecutionConfig().setCommit(version);
         if (measurementConfig.getExecutionConfig().getCommitOld() != null) {
            final String versionOld = GitUtils.getName(measurementConfig.getExecutionConfig().getCommitOld(), workspaceFolder);
            measurementConfig.getExecutionConfig().setCommitOld(versionOld);
         }
         return measurementConfig;
      } catch (Throwable e) {
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return null;
      }
   }
}
