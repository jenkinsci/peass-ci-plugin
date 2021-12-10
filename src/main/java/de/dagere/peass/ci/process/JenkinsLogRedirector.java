package de.dagere.peass.ci.process;

import de.dagere.peass.ci.logHandling.LogRedirector;
import hudson.model.TaskListener;

public class JenkinsLogRedirector implements AutoCloseable {
   
   private final LogRedirector redirector;
   
   public JenkinsLogRedirector(final TaskListener listener) {
      redirector = new LogRedirector(listener.getLogger());
   }

   @Override
   public void close() {
      redirector.close();
   }

}
