package de.peass.ci;

import java.util.Map.Entry;

import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peran.measurement.analysis.ProjectStatistics;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class MeasureVersionAction implements RunAction2 {

   private transient Run run;
   private MeasurementConfiguration config;
   private ProjectChanges changes;
   private ProjectStatistics statistics;

   public MeasureVersionAction(MeasurementConfiguration config, ProjectChanges changes, ProjectStatistics statistics) {
       this.config = config;
       this.changes = changes;
       this.statistics = statistics;
       for (Entry<String, Changes> change : changes.getVersionChanges().entrySet()) {
          System.out.println(change.getKey());
       }
   }

   @Override
   public String getIconFileName() {
      return "document.png";
   }

   @Override
   public String getDisplayName() {
      return "Performance Measurement";
   }

   @Override
   public String getUrlName() {
      return "measurement";
   }
   
   public MeasurementConfiguration getConfig() {
      return config;
   }
   
   public ProjectStatistics getStatistics() {
      return statistics;
   }
   
   public ProjectChanges getChanges() {
      return changes;
   }
   
   @Override
   public void onAttached(Run<?, ?> run) {
       this.run = run; 
   }

   @Override
   public void onLoad(Run<?, ?> run) {
       this.run = run; 
   }

   public Run getRun() { 
       return run;
   }
}
