package de.dagere.peass.ci.peassOverview;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;

public class PeassOverviewUtils {
   public static void removeNotTraceSelectedTests(StaticTestSelection selection, ExecutionData data) {
      for (String commit : selection.getCommitNames()) {
         TestSet commitTraceSelection = data.getCommits().get(commit);
         if (commitTraceSelection != null) {
            CommitStaticSelection commitStaticSelection = selection.getCommits().get(commit);
            if (commitStaticSelection != null) {
               for (Entry<Type, TestSet> changedEntity : commitStaticSelection.getChangedClazzes().entrySet()) {
                  Set<TestMethodCall> tests = new HashSet<>(changedEntity.getValue().getTestMethods());

                  for (TestMethodCall test : tests) {
                     if (!commitTraceSelection.getTestMethods().contains(test)) {
                        if (test.getMethod() != null) {
                           changedEntity.getValue().removeTest(test.onlyClazz(), test.getMethod());
                        } else {
                           changedEntity.getValue().removeTest(test.onlyClazz());
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
