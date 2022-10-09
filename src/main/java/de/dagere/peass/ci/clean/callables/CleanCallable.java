package de.dagere.peass.ci.clean.callables;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;

import de.dagere.peass.ci.process.JenkinsLogRedirector;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public abstract class CleanCallable implements FileCallable<Boolean> {

   private static final long serialVersionUID = -805865951563955476L;

   private final TaskListener listener;

   public CleanCallable(final TaskListener listener) {
      this.listener = listener;
   }

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {

   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) throws IOException, InterruptedException {
      try (final JenkinsLogRedirector redirector = new JenkinsLogRedirector(listener)) {
         final String projectName = potentialSlaveWorkspace.getName();
         final File folder = new File(potentialSlaveWorkspace.getParentFile(), projectName + PeassFolders.PEASS_FULL_POSTFIX);
         final ResultsFolders resultsFolders = new ResultsFolders(folder, projectName);

         cleanFolder(resultsFolders);

         return true;
      } catch (IOException e) {
         listener.getLogger().println("Exception thrown");
         e.printStackTrace(listener.getLogger());
         e.printStackTrace();
         return false;
      }
   }

   abstract void cleanFolder(final ResultsFolders resultsFolders) throws IOException;

}
