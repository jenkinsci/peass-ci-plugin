package de.dagere.peass.ci.peassOverview;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;

public class PeassOverviewUtils {
   public static void removeNotTraceSelectedTests(StaticTestSelection selection, ExecutionData data) {
      String newestVersion = selection.getNewestCommit();
      TestSet newestVersionTraceSelection = data.getCommits().get(newestVersion);
      if (newestVersionTraceSelection != null) {
         CommitStaticSelection versionStaticSelection = selection.getCommits().get(newestVersion);
         if (versionStaticSelection != null) {
            for (Map.Entry<ChangedEntity, TestSet> changedEntity : versionStaticSelection.getChangedClazzes().entrySet()) {
               Set<TestCase> tests = new HashSet<>(changedEntity.getValue().getTests());

               for (TestCase test : tests) {
                  if (!newestVersionTraceSelection.getTests().contains(test)) {
                     if (test.getMethod() != null) {
                        changedEntity.getValue().removeTest(test.onlyClazz(), test.getMethod());
                     } else {
                        changedEntity.getValue().removeTest(test);
                     }
                  }
               }
            }
         }
      }
   }
}
