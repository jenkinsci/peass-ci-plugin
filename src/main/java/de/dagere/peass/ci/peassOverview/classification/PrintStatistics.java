package de.dagere.peass.ci.peassOverview.classification;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.utils.Constants;

/**
 * Small helper for quickly getting the statistics of a classification
 *
 */
public class PrintStatistics {
   public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
      File classificationFile = new File(args[0]);

      Classifications classifications = Constants.OBJECTMAPPER.readValue(classificationFile, Classifications.class);

      
      Map<String, Integer> categoryCount = getCategoryCount(classifications);
      Map<String, Integer> unmeasuredCount = getUnmeasuredCount(classifications);
      
      System.out.println("Categories: " + categoryCount);
      System.out.println("Unmeasured: " + unmeasuredCount);
   }

   private static Map<String, Integer> getCategoryCount(Classifications classifications) {
      Map<String, Integer> categoryCount = new HashMap<>();
      for (ClassifiedProject project : classifications.getProjects().values()) {
         for (TestcaseClassification classification : project.getChangeClassifications().values()) {
            incrementCategoryCount(categoryCount, classification);
         }
      }
      return categoryCount;
   }
   
   private static Map<String, Integer> getUnmeasuredCount(Classifications classifications) {
      Map<String, Integer> categoryCount = new HashMap<>();
      for (ClassifiedProject project : classifications.getProjects().values()) {
         for (TestcaseClassification classification : project.getUnmeasuredClassifications().values()) {
            incrementCategoryCount(categoryCount, classification);
         }
      }
      return categoryCount;
   }

   private static void incrementCategoryCount(Map<String, Integer> categoryCount, TestcaseClassification classification) {
      for (String type : classification.getClassifications().values()) {
         Integer count = categoryCount.get(type);
         if (count == null) {
            count = 0;
         }
         count++;
         categoryCount.put(type, count);
      }
   }
}
