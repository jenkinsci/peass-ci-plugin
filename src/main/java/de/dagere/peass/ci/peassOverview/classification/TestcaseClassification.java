package de.dagere.peass.ci.peassOverview.classification;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestcaseClassification {
   private Map<String, String> classifications = new LinkedHashMap<>();

   public String getClassificationValue(String test2) {
      String classification = classifications.get(test2);
      if (classification != null) {
         return classification;
      }
      return "TODO";
   }

}