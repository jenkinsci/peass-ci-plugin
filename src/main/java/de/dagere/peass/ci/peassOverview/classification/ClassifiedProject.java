package de.dagere.peass.ci.peassOverview.classification;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ClassifiedProject {
   private Map<String, TestcaseClassification> changeClassifications = new LinkedHashMap<>();
   private Map<String, TestcaseClassification> unmeasuredClassifications = new LinkedHashMap<>();

   public Map<String, TestcaseClassification> getChangeClassifications() {
      return changeClassifications;
   }

   public void setChangeClassifications(Map<String, TestcaseClassification> changeClassifications) {
      this.changeClassifications = changeClassifications;
   }

   public Map<String, TestcaseClassification> getUnmeasuredClassifications() {
      return unmeasuredClassifications;
   }

   public void setUnmeasuredClassifications(Map<String, TestcaseClassification> unmeasuredClassifications) {
      this.unmeasuredClassifications = unmeasuredClassifications;
   }

   @JsonIgnore
   public TestcaseClassification getClassification(String commit) {
      if (changeClassifications.get(commit) != null) {
         return changeClassifications.get(commit);
      }
      if (unmeasuredClassifications.get(commit) != null) {
         return unmeasuredClassifications.get(commit);
      }
      return null;
   }
}