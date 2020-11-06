package de.peass.ci;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import de.peass.ContinuousExecutionStarter;
import de.peass.RootCauseAnalysis;
import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.MeasurementConfigurationMixin;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.CauseSearcher;
import de.peass.measurement.rca.CauseSearcherComplete;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.Constants;
import de.peass.visualization.RCAGenerator;
import de.peran.measurement.analysis.ProjectStatistics;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jenkins.tasks.SimpleBuildStep;
import kieker.analysis.exception.AnalysisConfigurationException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep {

   private int VMs;
   private int iterations;
   private int warmup;
   private int repetitions;
   private int timeout;
   private double significanceLevel;

   private int versionDiff;
   private boolean useGC;

   @DataBoundConstructor
   public MeasureVersionBuilder(String test) {
      System.out.println("Initializing" + test);
   }

   @Override
   public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
      final MeasurementConfiguration config = getConfig();

      if (!workspace.exists()) {
         listener.getLogger().println("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().println("Executing on " + workspace.toString());
         PrintStream outOriginal = System.out;
         PrintStream errOriginal = System.err;
         final File projectFolder = new File(workspace.toString());
         final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, config, 1, true);
         ProjectChanges changes = null;
         try {
            System.setOut(listener.getLogger());
            System.setErr(listener.getLogger());

            executor.execute();

            changes = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "changes.json"), ProjectChanges.class);

            executeRCA(config, executor, changes);
            
            RCAGenerator generator = new RCAGenerator(executor.getFolders().getFullMeasurementFolder().getParentFile(), executor.getLocalFolder());
         } catch (Throwable e) {
            e.printStackTrace();
         } finally {
            System.setOut(outOriginal);
            System.setErr(errOriginal);
         }
         ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "statistics.json"), ProjectStatistics.class);

         run.addAction(new MeasureVersionAction(config, changes, statistics));
      }
   }

   private void executeRCA(final MeasurementConfiguration config, final ContinuousExecutor executor, ProjectChanges changes)
         throws IOException, InterruptedException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      config.setVersion(executor.getLatestVersion());
      config.setVersionOld(executor.getVersionOld());
      
      Changes versionChanges = changes.getVersion(executor.getLatestVersion());
      for (Entry<String, List<Change>> testcases : versionChanges.getTestcaseChanges().entrySet()) {
         for (Change change : testcases.getValue()) {
            final TestCase testCase = new TestCase(testcases.getKey() + "#" + change.getMethod());
            final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(testCase, true, true, 5.0, true, 0.01, false, true);
            config.setUseKieker(true);
            
            final CauseSearchFolders alternateFolders = new CauseSearchFolders(executor.getFolders().getProjectFolder());
            final JUnitTestTransformer testtransformer = new JUnitTestTransformer(executor.getFolders().getProjectFolder(), config);
            final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, config, alternateFolders);
            final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
            final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, config, alternateFolders);
            tester.search();
         }
      }
   }

   private MeasurementConfiguration getConfig() {
      final MeasurementConfiguration config = new MeasurementConfiguration(timeout, VMs, significanceLevel, 0.01);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      System.out.println("Building, iterations: " + iterations);
      return config;
   }

   public int getVMs() {
      return VMs;
   }

   @DataBoundSetter
   public void setVMs(int vMs) {
      VMs = vMs;
   }

   public int getIterations() {
      return iterations;
   }

   @DataBoundSetter
   public void setIterations(int iterations) {
      System.out.println("Setting: " + iterations);
      this.iterations = iterations;
   }

   public int getWarmup() {
      return warmup;
   }

   @DataBoundSetter
   public void setWarmup(int warmup) {
      this.warmup = warmup;
   }

   public int getRepetitions() {
      return repetitions;
   }

   @DataBoundSetter
   public void setRepetitions(int repetitions) {
      this.repetitions = repetitions;
   }

   public int getTimeout() {
      return timeout;
   }

   @DataBoundSetter
   public void setTimeout(int timeout) {
      this.timeout = timeout;
   }

   public double getSignificanceLevel() {
      return significanceLevel;
   }

   @DataBoundSetter
   public void setSignificanceLevel(double significanceLevel) {
      this.significanceLevel = significanceLevel;
   }

   public int getVersionDiff() {
      return versionDiff;
   }

   @DataBoundSetter
   public void setVersionDiff(int versionDiff) {
      this.versionDiff = versionDiff;
   }

   public boolean isUseGC() {
      return useGC;
   }

   @DataBoundSetter
   public void setUseGC(boolean useGC) {
      this.useGC = useGC;
   }

   @Symbol("measure")
   @Extension
   public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

      public FormValidation doCheckName(@QueryParameter String value,
            @QueryParameter boolean useFrench)
            throws IOException, ServletException {
         if (value.length() == 0)
            return FormValidation.error("Strange value: " + value);
         return FormValidation.ok();
      }

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> aClass) {
         return true;
      }

      @Override
      public String getDisplayName() {
         return Messages.MeasureVersion_DescriptorImpl_DisplayName();
      }

   }

}
