package de.peass.ci.remote;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.remoting.RoleChecker;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.ContinuousFolderUtil;
import de.peass.ci.LogRedirector;
import de.peass.ci.helper.RCAExecutor;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcherConfig;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RemoteRCA implements FileCallable<Boolean>, Serializable {

   private static final long serialVersionUID = 5375409887559433077L;

   private final MeasurementConfiguration measurementConfig;
   private final CauseSearcherConfig causeConfig;
   private final ProjectChanges changes;
   private final EnvironmentVariables env;
   private final TaskListener listener;

   public RemoteRCA(final MeasurementConfiguration measurementConfig, final CauseSearcherConfig causeConfig, final ProjectChanges changes, final TaskListener listener,
         final EnvironmentVariables env) {
      this.measurementConfig = measurementConfig;
      this.causeConfig = causeConfig;
      this.changes = changes;
      this.listener = listener;
      this.env = env;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      final File localFolder = ContinuousFolderUtil.getLocalFolder(workspaceFolder);
      final File logFile = new File(localFolder, "rca_" + measurementConfig.getVersion() + ".txt");
      listener.getLogger().println("Executing root cause analysis - Log goes to " + logFile.getAbsolutePath());
      try (LogRedirector director = new LogRedirector(logFile)) {
         final File projectFolderLocal = new File(localFolder, workspaceFolder.getName());
         final RCAExecutor rcaExecutor = new RCAExecutor(measurementConfig, projectFolderLocal, changes, causeConfig, env);
         rcaExecutor.executeRCAs();
         return true;
      } catch (XmlPullParserException | AnalysisConfigurationException | ViewNotFoundException | JAXBException e) {
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
