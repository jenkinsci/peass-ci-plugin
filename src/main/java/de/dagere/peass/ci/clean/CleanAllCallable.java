package de.dagere.peass.ci.clean;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class CleanAllCallable implements FileCallable<Boolean> {
   private static final long serialVersionUID = 3804971173610549315L;

   @Override
   public void checkRoles(final RoleChecker checker) throws SecurityException {
   }

   @Override
   public Boolean invoke(final File potentialSlaveWorkspace, final VirtualChannel channel) {
      try {
         File folder = new File(potentialSlaveWorkspace.getParentFile(), potentialSlaveWorkspace.getName() + "_fullPeass");
         if (folder.exists()) {
            System.out.println("Cleaning " + folder.getAbsolutePath());
            FileUtils.cleanDirectory(folder);
         } else {
            FileUtils.cleanDirectory(potentialSlaveWorkspace);
         }
         return true;
      } catch (IOException | IllegalArgumentException e) {
         e.printStackTrace();
         return false;
      }
   }
}
