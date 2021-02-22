package de.peass.ci;

import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Project;
import jenkins.model.TransientActionFactory;

@Extension
public class ProjectStatisticsActionFactory extends TransientActionFactory<Project> {

   @Override
   public Class<Project> type() {
      // This will only apply to Project instances.
      return Project.class;
   }

   @NonNull
   @Override
   public Collection<? extends Action> createFor(@NonNull final Project project) {

      return Collections.singleton(new ProjectStatisticsAction(project));
   }

}