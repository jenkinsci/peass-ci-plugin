package de.dagere.peass.ci.peassOverview;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.peassOverview.classification.Classifications;
import de.dagere.peass.ci.peassOverview.classification.ClassifiedProject;
import de.dagere.peass.ci.peassOverview.classification.TestcaseClassification;
import de.dagere.peass.utils.Constants;
import hudson.util.FormValidation;

public class PeassOverviewAction extends VisibleAction {

   private final Map<String, ProjectData> projects;
   private final String changeClassifications;
   private final String unmeasuredClassifications;
   private final String path;

   public PeassOverviewAction(int id, Map<String, ProjectData> projects, String changeClassifications, String unmeasuredClassifications, String path) {
      super(id);
      this.projects = projects;
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

}
