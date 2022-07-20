package de.dagere.peass.ci.peassOverview;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.QueryParameter;
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

   public PeassOverviewAction(int id, Map<String, ProjectData> projects, String changeClassifications, String unmeasuredClassifications) {
      super(id);
      this.projects = projects;
      this.changeClassifications = changeClassifications;
      this.unmeasuredClassifications = unmeasuredClassifications;

      File classificationFile = new File("classifications.json");
      Classifications classifications = new Classifications();
      for (String project : projects.keySet()) {
         classifications.getProjects().put(project, new ClassifiedProject());
      }
      try {
         Constants.OBJECTMAPPER.writeValue(classificationFile, Classifications.class);
      } catch (IOException e) {
         e.printStackTrace();
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
      File classificationFile = new File("classifications.json");
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
      File classificationFile = new File("classifications.json");
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

      File classificationFile = new File("classifications.json");
      try {
         Classifications classifications;
         if (classificationFile.exists()) {
            classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
         } else {
            classifications = new Classifications();
         }

         classifications.setClassification(project, commit, testcase, classification);

         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);

         return FormValidation.ok("Updated value");
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

      File classificationFile = new File("classifications.json");
      try {
         Classifications classifications;
         if (classificationFile.exists()) {
            classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);
         } else {
            classifications = new Classifications();
         }

         classifications.setUnmeasuredClassification(project, commit, testcase, classification);

         Constants.OBJECTMAPPER.writeValue(classificationFile, classifications);

         return FormValidation.ok("Updated value");
      } catch (IOException e) {
         System.out.println("Tried to write to " + classificationFile.getAbsolutePath());
         e.printStackTrace();
         return FormValidation.error("Some error occured");
      }
   }

}
