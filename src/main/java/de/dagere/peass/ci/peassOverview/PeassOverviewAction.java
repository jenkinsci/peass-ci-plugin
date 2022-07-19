package de.dagere.peass.ci.peassOverview;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import org.jvnet.localizer.LocaleProvider;

import de.dagere.peass.ci.VisibleAction;
import de.dagere.peass.ci.peassOverview.classification.Classifications;
import de.dagere.peass.ci.peassOverview.classification.ClassifiedProject;
import de.dagere.peass.ci.peassOverview.classification.TestcaseClassification;

public class PeassOverviewAction extends VisibleAction {

   private final Map<String, ProjectData> projects;
   private final Classifications classifications = new Classifications();
   private final String changeClassifications;
   private final String unmeasuredClassifications;

   public PeassOverviewAction(int id, Map<String, ProjectData> projects, String changeClassifications, String unmeasuredClassifications) {
      super(id);
      this.projects = projects;
      this.changeClassifications = changeClassifications;
      this.unmeasuredClassifications = unmeasuredClassifications;

      for (String project : projects.keySet()) {
         classifications.getProjects().put(project, new ClassifiedProject());
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
   
   public String[] getChangeClassificationArray(){
      return changeClassifications.split(";");
   }
   
   public String[] getUnmeasuredClassificationArray(){
      return unmeasuredClassifications.split(";");
   }
   
   public String getClassification(String project, String commit, String test ) {
      ClassifiedProject classifiedProject = classifications.getProjects().get(project);
      if (classifiedProject != null) {
         TestcaseClassification testcaseClassification = classifiedProject.getClassification(commit);
         if (testcaseClassification != null) {
            return testcaseClassification.getClassificationValue(test);
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

}
