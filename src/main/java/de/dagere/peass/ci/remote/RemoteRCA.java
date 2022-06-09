package de.dagere.peass.ci.remote;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.ContinuousFolderUtil;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.RCAExecutor;
import de.dagere.peass.ci.logHandling.LogRedirector;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import kieker.analysis.exception.AnalysisConfigurationException;

public class RemoteRCA implements FileCallable<RCAResult>, Serializable {

   private static final long serialVersionUID = 5375409887559433077L;

   private final MeasurementConfig measurementConfig;
   private final CauseSearcherConfig causeConfig;
   private final ProjectChanges changes;
   private final EnvironmentVariables env;
   private final TaskListener listener;
   private final List<TestCase> failedTests = new LinkedList<>();

   public RemoteRCA(final PeassProcessConfiguration peassConfig, final CauseSearcherConfig causeConfig, final ProjectChanges changes, final TaskListener listener) {
      this.measurementConfig = peassConfig.getMeasurementConfig();
      this.causeConfig = causeConfig;
      this.changes = changes;
      this.listener = listener;
      this.env = peassConfig.getEnvVars();
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public RCAResult invoke(final File workspaceFolder, final VirtualChannel channel) throws IOException, InterruptedException {
      final File localFolder = ContinuousFolderUtil.getLocalFolder(workspaceFolder);
      ResultsFolders resultsFolder = new ResultsFolders(localFolder, workspaceFolder.getName());
      final File logFile = resultsFolder.getRCALogFile(measurementConfig.getExecutionConfig().getCommit(), measurementConfig.getExecutionConfig().getCommitOld());
      if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
         listener.getLogger().println("Executing root cause analysis - Log goes to " + logFile.getAbsolutePath());
         try (LogRedirector director = new LogRedirector(logFile)) {
            executeRCA(workspaceFolder, localFolder, resultsFolder);
            return new RCAResult(true, failedTests);
         } catch (XmlPullParserException | AnalysisConfigurationException | ViewNotFoundException e) {
            File test = new File(workspaceFolder, "error.txt"); // Workaround, since error redirection on Jenkins agents currently does not work
            PrintStream writer = new PrintStream(test, "UTF-8");
            e.printStackTrace(writer);
            writer.flush();
            listener.getLogger().println("Exception thrown");
            e.printStackTrace(listener.getLogger());
            e.printStackTrace();
            return new RCAResult(false, failedTests);
         }
      } else {
         try {
            executeRCA(workspaceFolder, localFolder, resultsFolder);
            return new RCAResult(true, failedTests);
         } catch (IOException | InterruptedException | XmlPullParserException | AnalysisConfigurationException | ViewNotFoundException e) {
            e.printStackTrace();
            return  new RCAResult(false, failedTests);
         }
      }
   }

   private void executeRCA(final File workspaceFolder, final File localFolder, final ResultsFolders resultsFolder)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      final File projectFolderLocal = new File(localFolder, workspaceFolder.getName());
      File propertyFolder = resultsFolder.getPropertiesFolder();
      causeConfig.setPropertyFolder(propertyFolder);
      
      // Only one call recording is not allowed for RCA
      measurementConfig.getKiekerConfig().setOnlyOneCallRecording(false);
      
      listener.getLogger().println("Setting property folder: " + propertyFolder.getAbsolutePath());
      final RCAExecutor rcaExecutor = new RCAExecutor(measurementConfig, projectFolderLocal, changes, causeConfig, env);
      rcaExecutor.executeRCAs();
      
      failedTests.addAll(rcaExecutor.getFailedTests());
   }
   
   public List<TestCase> getFailedTests() {
      return failedTests;
   }

}
