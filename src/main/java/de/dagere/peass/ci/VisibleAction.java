package de.dagere.peass.ci;

import hudson.model.Run;
import jenkins.model.RunAction2;

public abstract class VisibleAction implements RunAction2 {

   private static int idCounter = 0;
   
   protected final int id;
   private transient Run<?, ?> run;
   
   public VisibleAction() {
      id = idCounter;
      idCounter++;
   }
   
   @Override
   public void onAttached(final Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(final Run<?, ?> run) {
      this.run = run;
   }
   
   /**
    * This getter is required for run side panel displaying, even if the method is not called and there is no override
    * @return
    */
   public Run<?, ?> getRun() {
      return run;
   }
}
