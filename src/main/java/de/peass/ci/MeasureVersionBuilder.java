package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.MeasurementMode;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.helper.HistogramReader;
import de.peass.ci.helper.HistogramValues;
import de.peass.ci.helper.RCAExecutor;
import de.peass.ci.helper.RCAVisualizer;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.utils.Constants;
import de.peran.measurement.analysis.ProjectStatistics;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import kieker.analysis.exception.AnalysisConfigurationException;

public class MeasureVersionBuilder extends Builder implements SimpleBuildStep {

   private int VMs;
   private int iterations;
   private int warmup;
   private int repetitions;
   private int timeout;
   private double significanceLevel;

   private int versionDiff;
   private boolean useGC;

   private String includes = "";
   private boolean executeRCA = true;
   private MeasurementMode measurementMode;

   @DataBoundConstructor
   public MeasureVersionBuilder(String test) {
      System.out.println("Initializing" + test);
   }

   @Override
   public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
      if (!workspace.exists()) {
         throw new RuntimeException("Workspace folder " + workspace.toString() + " does not exist, please asure that the repository was correctly cloned!");
      } else {
         listener.getLogger().println("Executing on " + workspace.toString());
         listener.getLogger().println("VMs: " + VMs + " Iterations: " + iterations + " Warmup: " + warmup + " Repetitions: " + repetitions);
         listener.getLogger().println("Includes: " + includes + " RCA: " + executeRCA);

         final PrintStream outOriginal = System.out;
         final PrintStream errOriginal = System.err;
         
         final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
         
         OutputStreamAppender fa = OutputStreamAppender.newBuilder()
               .setName("jenkinslogger")
               .setTarget(listener.getLogger())
               .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n")
               .build())
               .setConfiguration(loggerContext.getConfiguration()).build();
         fa.start();

         try {
            System.setOut(listener.getLogger());
            System.setErr(listener.getLogger());
            
            loggerContext.getConfiguration().addAppender(fa);
            loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(fa.getName()));
            loggerContext.updateLoggers();
            
            performExecution(run, workspace);
         } catch (Throwable e) {
            e.printStackTrace();
            run.setResult(Result.FAILURE);
         } finally {
            System.setOut(outOriginal);
            System.setErr(errOriginal);

            fa.stop();
            loggerContext.getConfiguration().getAppenders().remove(fa.getName());
            loggerContext.getRootLogger().removeAppender(fa);
         }
      }
   }

   private void performExecution(Run<?, ?> run, FilePath workspace) throws InterruptedException, IOException, JAXBException, XmlPullParserException, JsonParseException,
         JsonMappingException, AnalysisConfigurationException, ViewNotFoundException, Exception {
      final MeasurementConfiguration measurementConfig = getConfig();

      final File projectFolder = new File(workspace.toString());
      final ContinuousExecutor executor = new ContinuousExecutor(projectFolder, measurementConfig, 1, true);
      List<String> includeList = getIncludeList();
      executor.execute(includeList);

      final HistogramReader histogramReader = new HistogramReader(executor);
      Map<String, HistogramValues> measurements = histogramReader.readMeasurements();

      final ProjectChanges changes = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "changes.json"), ProjectChanges.class);

      if (executeRCA) {
         RCAExecutor rcaExecutor = new RCAExecutor(measurementConfig, executor, changes, measurementMode, includeList);
         rcaExecutor.executeRCAs();

         RCAVisualizer rcaVisualizer = new RCAVisualizer(executor, changes, run);
         rcaVisualizer.visualizeRCA();
      }

      ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(new File(executor.getLocalFolder(), "statistics.json"), ProjectStatistics.class);

      final MeasureVersionAction action = new MeasureVersionAction(measurementConfig, changes, statistics, measurements);
      run.addAction(action);
   }

   private List<String> getIncludeList() {
      List<String> includeList = new LinkedList<>();
      if (includes != null && includes.trim().length() > 0) {
         final String nonSpaceIncludes = includes.replaceAll(" ", "");
         for (String include : nonSpaceIncludes.split(";")) {
            includeList.add(include);
         }
      }
      return includeList;
   }

   private MeasurementConfiguration getConfig() {
      final MeasurementConfiguration config = new MeasurementConfiguration(timeout, VMs, significanceLevel, 0.01);
      config.setIterations(iterations);
      config.setWarmup(warmup);
      config.setRepetitions(repetitions);
      config.setUseGC(useGC);
      config.setEarlyStop(false);
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

   public String getIncludes() {
      return includes;
   }

   @DataBoundSetter
   public void setIncludes(String includes) {
      this.includes = includes;
   }

   public boolean isExecuteRCA() {
      return executeRCA;
   }

   @DataBoundSetter
   public void setExecuteRCA(boolean executeRCA) {
      this.executeRCA = executeRCA;
   }

   public MeasurementMode getMeasurementMode() {
      return measurementMode;
   }

   public void setMeasurementMode(MeasurementMode measurementMode) {
      this.measurementMode = measurementMode;
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
