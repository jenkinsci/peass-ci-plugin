package de.dagere.peass.ci.peassOverview.classification;

import java.util.LinkedHashMap;
import java.util.Map;

public class Classifications {
   private Map<String, ClassifiedProject> projects = new LinkedHashMap<>();

   public Map<String, ClassifiedProject> getProjects() {
      return projects;
   }

   public void setProjects(Map<String, ClassifiedProject> projects) {
      this.projects = projects;
   }
}
