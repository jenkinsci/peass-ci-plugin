package de.peass.ci;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.ci.persistence.BuildMeasurementValues;
import de.peass.ci.persistence.TestMeasurementValues;
import de.peass.ci.persistence.TrendFileUtil;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Project;

public class ProjectStatisticsAction implements Action {

   private Job<?, ?> project;

   public ProjectStatisticsAction(final Job<?, ?> project) {
      this.project = project;
   }

   public int getBuildStepsCount() {
      if (project instanceof WorkflowJob) {
         WorkflowJob job = (WorkflowJob) project;
         return job.getBuilds().size();
         // WorkflowJob job = project;
      } else if (project instanceof Project) {
         return ((Project) project).getBuilders().size();
      } else {
         return 0;
      }
   }

   /**
    * Gets a mapping from buildnumber to mean value of the measurement of the build; this returns a LinkedHashMap since the order of the values needs to be guaranteed
    */
   public LinkedHashMap<Integer, Double> getMeanMap(final String testcase) throws InterruptedException, IOException {
      final LinkedHashMap<Integer, Double> meanMap = new LinkedHashMap<>();

      BuildMeasurementValues values = readValues();

      TestMeasurementValues testMeasurementValues = values.getValues().get(testcase);
      for (Map.Entry<Integer, TestcaseStatistic> vals : testMeasurementValues.getStatistics().entrySet()) {
         meanMap.put(vals.getKey(), vals.getValue().getMeanCurrent());
      }

      return meanMap;
   }

   public LinkedHashMap<Integer, Double> getLowerBound(final String testcase) throws JsonParseException, JsonMappingException, InterruptedException, IOException {
      final LinkedHashMap<Integer, Double> deviationMap = new LinkedHashMap<>();

      BuildMeasurementValues values = readValues();

      TestMeasurementValues testMeasurementValues = values.getValues().get(testcase);
      for (Map.Entry<Integer, TestcaseStatistic> vals : testMeasurementValues.getStatistics().entrySet()) {
         deviationMap.put(vals.getKey(), vals.getValue().getMeanCurrent() - vals.getValue().getDeviationCurrent());
      }

      return deviationMap;
   }

   public LinkedHashMap<Integer, Double> getUpperBound(final String testcase) throws JsonParseException, JsonMappingException, InterruptedException, IOException {
      final LinkedHashMap<Integer, Double> deviationMap = new LinkedHashMap<>();

      BuildMeasurementValues values = readValues();

      TestMeasurementValues testMeasurementValues = values.getValues().get(testcase);
      for (Map.Entry<Integer, TestcaseStatistic> vals : testMeasurementValues.getStatistics().entrySet()) {
         deviationMap.put(vals.getKey(), vals.getValue().getMeanCurrent() + vals.getValue().getDeviationCurrent());
      }

      return deviationMap;
   }

   public Set<String> getTestcases() throws JsonParseException, JsonMappingException, InterruptedException, IOException {
      BuildMeasurementValues values = readValues();
      return values.getValues().keySet();
   }

   public String getBuildnumbersReadable(final String testcase) throws InterruptedException, IOException {
      return getMeanMap(testcase).keySet().toString();
   }

   public String getMeansReadable(final String testcase) throws InterruptedException, IOException {
      return getMeanMap(testcase).values().toString();
   }

   public String getLowerBoundReadable(final String testcase) throws InterruptedException, IOException {
      return getLowerBound(testcase).values().toString();
   }

   public String getUpperBoundReadable(final String testcase) throws InterruptedException, IOException {
      return getUpperBound(testcase).values().toString();
   }

   private BuildMeasurementValues readValues() throws InterruptedException, IOException, JsonParseException, JsonMappingException {
      if (project instanceof WorkflowJob || project instanceof Project) {
         BuildMeasurementValues values = TrendFileUtil.readMeasurementValues(project.getName());
         return values;
      } else {
         return null;
      }
   }

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/rca.png";
   }

   @Override
   public String getDisplayName() {
      return "Project Statistics";
   }

   @Override
   public String getUrlName() {
      return "stats";
   }
}