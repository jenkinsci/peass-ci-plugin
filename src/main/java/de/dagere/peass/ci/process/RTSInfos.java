package de.dagere.peass.ci.process;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class RTSInfos {
   private final boolean staticChanges;
   private final boolean staticallySelectedTests;
   private final TestSet ignoredTestsCurrent;
   private final TestSet ignoredTestsPredecessor;

   public RTSInfos(final boolean staticChanges, final boolean staticallySelectedTests, final TestSet ignoredTestsCurrent, final TestSet ignoredTestsPredecessor) {
      this.staticChanges = staticChanges;
      this.staticallySelectedTests = staticallySelectedTests;
      this.ignoredTestsCurrent = ignoredTestsCurrent;
      this.ignoredTestsPredecessor = ignoredTestsPredecessor;
   }

   public boolean isStaticChanges() {
      return staticChanges;
   }

   public boolean isStaticallySelectedTests() {
      return staticallySelectedTests;
   }

   public TestSet getIgnoredTestsCurrent() {
      return ignoredTestsCurrent;
   }

   public TestSet getIgnoredTestsPredecessor() {
      return ignoredTestsPredecessor;
   }

   public static RTSInfos readInfosFromFolders(final ResultsFolders results, final PeassProcessConfiguration peassConfig)
         throws StreamReadException, DatabindException, IOException {
      File staticTestSelectionFile = results.getStaticTestSelectionFile();
      if (staticTestSelectionFile.exists()) {
         boolean staticChanges = false;
         StaticTestSelection staticTestSelection = Constants.OBJECTMAPPER.readValue(staticTestSelectionFile, StaticTestSelection.class);
         CommitStaticSelection version = staticTestSelection.getCommits().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());
         boolean hasStaticallySelectedTests = false;

         TestSet ignoredTestsCurrent = new TestSet();
         if (version != null) {
            if (!version.getChangedClazzes().isEmpty()) {
               staticChanges = true;
            }
            TestSet tests = version.getTests();
            hasStaticallySelectedTests = !tests.getTests().isEmpty();
            ignoredTestsCurrent = version.getIgnoredAffectedTests();
         }

         CommitStaticSelection predecessor = staticTestSelection.getCommits().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommitOld());
         TestSet ignoredTestsPredecessor;
         if (predecessor != null) {
            ignoredTestsPredecessor = predecessor.getIgnoredAffectedTests();
         } else {
            ignoredTestsPredecessor = new TestSet();
         }

         return new RTSInfos(staticChanges, hasStaticallySelectedTests, ignoredTestsCurrent, ignoredTestsPredecessor);
      } else {
         return new RTSInfos(false, false, new TestSet(), new TestSet());
      }
   }
}
