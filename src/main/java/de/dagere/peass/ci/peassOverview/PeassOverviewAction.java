package de.dagere.peass.ci.peassOverview;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.peassOverview.classification.Classifications;
import de.dagere.peass.ci.peassOverview.classification.ClassifiedProject;
import de.dagere.peass.ci.peassOverview.classification.TestcaseClassification;
import de.dagere.peass.ci.rca.CommitRCAURLs;
import de.dagere.peass.ci.rca.RCAMapping;
import de.dagere.peass.utils.Constants;
import hudson.util.FormValidation;

public class PeassOverviewAction extends VisibleAction {

   private static final Logger LOG = LogManager.getLogger(PeassOverviewAction.class);

   private final Map<String, ProjectData> projects;
   private final Map<String, RCAMapping> projectRCAMappings;
   private final String changeClassifications;
   private final String unmeasuredClassifications;
   private final String path;

   public PeassOverviewAction(int id, Map<String, ProjectData> projects, Map<String, RCAMapping> projectRCAMappings, String changeClassifications, String unmeasuredClassifications, String path) {
      super(id);
      this.projects = projects;
      this.projectRCAMappings = projectRCAMappings;
      this.changeClassifications = changeClassifications;
      this.unmeasuredClassifications = unmeasuredClassifications;
      this.path = path;

      File parentFile = new File(path);
      if (!parentFile.exists()) {
         if (!parentFile.mkdirs()) {
            throw new RuntimeException("Creating " + path + " was not possible!");
         }
      }
      File classificationFile = new File(parentFile, "classifications.json");
      if (!classificationFile.exists()) {
         writeEmptyClassifications(projects, classificationFile);
      } else {
         removeUnneededClassifications(projects, classificationFile);
      }
   }

   private void writeEmptyClassifications(Map<String, ProjectData> projects, File classificationFile) {
      Classifications classifications = new Classifications();
      for (String project : projects.keySet()) {
         classifications.getProjects().put(project, new ClassifiedProject());
      }
      try {
         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void removeUnneededClassifications(Map<String, ProjectData> projects, File classificationFile) {
      try {
         Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
         for (Entry<String, ClassifiedProject> project : classifications.getProjects().entrySet()) {
            ProjectData data = projects.get(project.getKey());
            if (data != null) {
               for (Entry<String, TestcaseClassification> commitClassification : project.getValue().getChangeClassifications().entrySet()) {
                  Changes commitChanges = data.getChanges().getCommitChanges(commitClassification.getKey());
                  Set<String> deletables = new HashSet<>();
                  for (Entry<String, String> testcaseClassification : commitClassification.getValue().getClassifications().entrySet()) {
                     TestMethodCall testMethod = TestMethodCall.createFromString(testcaseClassification.getKey());
                     Change change = commitChanges.getChange(testMethod);
                     if (change == null) {
                        System.out.println("Deletable: " + testMethod + " " + commitClassification.getKey());
                        deletables.add(testcaseClassification.getKey());
                     }
                  }
                  for (String deletable : deletables) {
                     commitClassification.getValue().getClassifications().remove(deletable);
                  }
               }
            }
         }
         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public RCAMapping getRCAMapping(String project) {
      return projectRCAMappings.get(project);
   }
   
   public String getRCAUrl(String project, String commit, String testcase) {
      RCAMapping rcaMapping = projectRCAMappings.get(project);
      if (rcaMapping != null) {
         System.out.println("Mapping detected");
         CommitRCAURLs commitMapping = rcaMapping.getCommits().get(commit);
         if (commitMapping != null) {
            TestMethodCall testMethod = TestMethodCall.createFromString(testcase);
            System.out.println("Method: " + testMethod);
            String url = commitMapping.getExecutionURLs().get(testMethod);
            System.out.println("URL: " + url);
            return url;
         }
      }
      return null;
   }

   public Map<String, ProjectData> getProjects() {
      return projects;
   }

   public String getChangeClassifications() {
      return changeClassifications;
   }

   public String getUnmeasuredClassifications() {
      return unmeasuredClassifications;
   }

   public String[] getChangeClassificationArray() {
      return changeClassifications.split(";");
   }

   public String[] getUnmeasuredClassificationArray() {
      return unmeasuredClassifications.split(";");
   }

   public String getClassification(String project, String commit, String test) {
      File classificationFile = new File(path, "classifications.json");
      if (classificationFile.exists()) {
         try {
            Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
            ClassifiedProject classifiedProject = classifications.getProjects().get(project);
            if (classifiedProject != null) {
               TestcaseClassification testcaseClassification = classifiedProject.getClassification(commit);
               if (testcaseClassification != null) {
                  return testcaseClassification.getClassificationValue(test);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return "TODO";
   }

   public String getUnmeasuredClassification(String project, String commit, String test) {
      File classificationFile = new File(path, "classifications.json");
      if (classificationFile.exists()) {
         try {
            Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
            ClassifiedProject classifiedProject = classifications.getProjects().get(project);
            if (classifiedProject != null) {
               TestcaseClassification testcaseClassification = classifiedProject.getUnmeasuredClassification(commit);
               if (testcaseClassification != null) {
                  return testcaseClassification.getClassificationValue(test);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return "TODO";
   }

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/overview.png";
   }

   @Override
   public String getDisplayName() {
      return "Peass Measurement Overview";
   }

   @Override
   public String getUrlName() {
      return "overview";
   }

   public String round(final double value) {
      double roundedValue = Math.round(value * 10000) / 10000d;
      Locale locale = LocaleProvider.getLocale();
      return NumberFormat.getInstance(locale).format(roundedValue);
   }

   public Map<String, ProjectOverviewStatistic> getStatistic() {
      Map<String, ProjectOverviewStatistic> result = new LinkedHashMap<>();

      File classificationFile = new File(path, "classifications.json");
      if (classificationFile.exists()) {
         try {
            Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
            for (Map.Entry<String, ClassifiedProject> project : classifications.getProjects().entrySet()) {
               String projectName = project.getKey();
               LOG.info("Adding project " + projectName);
               ProjectOverviewStatistic statistic = ProjectOverviewStatistic.getFromClassification(projectName, projects.get(projectName), project.getValue());

               result.put(projectName, statistic);
            }
            ProjectOverviewStatistic mergedStatistic = ProjectOverviewStatistic.getSumStatistic(result.values());
            result.put("Sum", mergedStatistic);

         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("Classification file did not exist");
      }

      return result;
   }

   @RequirePOST
   public FormValidation doUpdateClassification(@QueryParameter String project,
         @QueryParameter String commit,
         @QueryParameter String testcase,
         @QueryParameter String classification) {

      File classificationFile = new File(path, "classifications.json");
      try {
         Classifications classifications;
         if (classificationFile.exists()) {
            classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
         } else {
            classifications = new Classifications();
         }

         classifications.setClassification(project, commit, testcase, classification);

         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);

         return FormValidation.ok("Updated value of " + testcase + " (" + commit + ") to " + classification);
      } catch (IOException e) {
         System.out.println("Tried to write to " + classificationFile.getAbsolutePath());
         e.printStackTrace();
         return FormValidation.error("Some error occured");
      }
   }

   @RequirePOST
   public FormValidation doUpdateUnmeasured(@QueryParameter String project,
         @QueryParameter String commit,
         @QueryParameter String testcase,
         @QueryParameter String classification) {

      File classificationFile = new File(path, "classifications.json");
      try {
         Classifications classifications;
         if (classificationFile.exists()) {
            classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
         } else {
            classifications = new Classifications();
         }

         classifications.setUnmeasuredClassification(project, commit, testcase, classification);

         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);

         return FormValidation.ok("Updated value of " + testcase + " (" + commit + ") to " + classification);
      } catch (IOException e) {
         System.out.println("Tried to write to " + classificationFile.getAbsolutePath());
         e.printStackTrace();
         return FormValidation.error("Some error occured");
      }
   }

   public HttpResponse doDownloadClassification() {
      return new HttpResponse() {

         @Override
         public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
            rsp.addHeader("Content-Type", "application/json");

            File classificationFile = new File(path, "classifications.json");
            Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);

            String responseText = Constants.OBJECTMAPPER.writeValueAsString(classifications);
            byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
            InputStream stream = new ByteArrayInputStream(responseBytes);

            rsp.serveFile(req, stream, System.currentTimeMillis(), (long) responseBytes.length, "classifications.json");

         }
      };

   }

   public HttpResponse doDownloadUnclassified() {
      return new HttpResponse() {

         @Override
         public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
            rsp.addHeader("Content-Type", "application/json");

            Classifications unclassifiedList = buildUnclassifiedList();

            String responseText = Constants.OBJECTMAPPER.writeValueAsString(unclassifiedList);
            byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
            InputStream stream = new ByteArrayInputStream(responseBytes);

            rsp.serveFile(req, stream, System.currentTimeMillis(), (long) responseBytes.length, "unclassified.json");

         }

         private Classifications buildUnclassifiedList() {
            Classifications unclassifiedClassification = new Classifications();

            for (Entry<String, ProjectData> project : projects.entrySet()) {
               String projectName = project.getKey();
               for (Entry<String, Changes> commit : project.getValue().getChanges().getCommitChanges().entrySet()) {
                  for (Entry<String, List<Change>> changes : commit.getValue().getTestcaseChanges().entrySet()) {
                     for (Change change : changes.getValue()) {
                        String testcase = changes.getKey() + "#" + change.getMethod();
                        String commitName = commit.getKey();

                        if (getClassification(projectName, commitName, testcase).equals("TODO")) {
                           unclassifiedClassification.setClassification(projectName, commitName, testcase, "TODO");
                        }
                     }
                  }
               }
            }
            return unclassifiedClassification;
         }
      };

   }

}
