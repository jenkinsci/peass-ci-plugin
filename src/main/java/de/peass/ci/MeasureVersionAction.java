package de.peass.ci;

import de.peass.dependency.execution.MeasurementConfiguration;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class MeasureVersionAction implements RunAction2 {

   private transient Run run;
   private MeasurementConfiguration config;

   public MeasureVersionAction(MeasurementConfiguration config) {
       this.config = config;
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
