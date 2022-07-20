package de.dagere.peass.ci.peassOverview.classification;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TestcaseClassification {
   private Map<String, String> classifications = new LinkedHashMap<>();

   public Map<String, String> getClassifications() {
      return classifications;
   }
   
   public void setClassifications(Map<String, String> classifications) {
      this.classifications = classifications;
   }
   
   @JsonIgnore
   public void setClassificationValue(String testcase, String classification) {
      classifications.put(testcase, classification);
   }
   
   @JsonIgnore
   public String getClassificationValue(String test2) {
      String classification = classifications.get(test2);
      if (classification != null) {
         return classification;
      }
      return "TODO";
   }

}