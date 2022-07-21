package de.dagere.peass.ci.peassOverview.classification;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Classifications {
   private Map<String, ClassifiedProject> projects = new LinkedHashMap<>();

   public Map<String, ClassifiedProject> getProjects() {
      return projects;
   }

   public void setProjects(Map<String, ClassifiedProject> projects) {
      this.projects = projects;
   }

   @JsonIgnore
   public void setClassification(String project, String commit, String testcase, String classification) {
      ClassifiedProject projectObject = projects.get(project);
      if (projectObject == null) {
         projectObject = new ClassifiedProject();
         projects.put(project, projectObject);
      }
      TestcaseClassification testcaseClassifications = projectObject.getClassification(commit);
      testcaseClassifications.setClassificationValue(testcase, classification);
   }
   
   @JsonIgnore
   public void setUnmeasuredClassification(String project, String commit, String testcase, String classification) {
      ClassifiedProject projectObject = projects.get(project);
      if (projectObject == null) {
         projectObject = new ClassifiedProject();
         projects.put(project, projectObject);
      }
      TestcaseClassification testcaseClassifications = projectObject.getUnmeasuredClassification(commit);
      testcaseClassifications.setClassificationValue(testcase, classification);
   }
}
