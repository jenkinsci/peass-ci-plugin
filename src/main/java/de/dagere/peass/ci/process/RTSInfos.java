package de.dagere.peass.ci.process;

import java.io.File;
import java.io.IOException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

/**
 * Stores results of one RTS run - it is necessary to store these reduced data instead of StaticTestSelection, so not all data get persisted for every commit (otherwise, this would be a lot of data).
 * @author DaGeRe
 *
 */
public class RTSInfos {
   private final boolean staticChanges;
   private final boolean staticallySelectedTests;
   private final TestSet ignoredTestsCurrent;
   private final TestSet ignoredTestsPredecessor;
   private final TestSet removedTestsCurrent;

   public RTSInfos(final boolean staticChanges, final boolean staticallySelectedTests, final TestSet ignoredTestsCurrent, final TestSet ignoredTestsPredecessor, TestSet removedTestsCurrent) {
      this.staticChanges = staticChanges;
      this.staticallySelectedTests = staticallySelectedTests;
      this.ignoredTestsCurrent = ignoredTestsCurrent;
      this.ignoredTestsPredecessor = ignoredTestsPredecessor;
      this.removedTestsCurrent = removedTestsCurrent;
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
   
   public TestSet getRemovedTestsCurrent() {
      return removedTestsCurrent;
   }

   public static RTSInfos readInfosFromFolders(final ResultsFolders results, final PeassProcessConfiguration peassConfig) throws IOException {
      File staticTestSelectionFile = results.getStaticTestSelectionFile();
      if (staticTestSelectionFile.exists()) {
         boolean staticChanges = false;
         StaticTestSelection staticTestSelection = Constants.OBJECTMAPPER.readValue(staticTestSelectionFile, StaticTestSelection.class);
         CommitStaticSelection commitSelection = staticTestSelection.getCommits().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommit());
         boolean hasStaticallySelectedTests = false;

         TestSet ignoredTestsCurrent = new TestSet();
         if (commitSelection != null) {
            if (!commitSelection.getChangedClazzes().isEmpty()) {
               staticChanges = true;
            }
            TestSet tests = commitSelection.getTests();
            hasStaticallySelectedTests = !tests.getTests().isEmpty();
            ignoredTestsCurrent = commitSelection.getIgnoredAffectedTests();
         }

         CommitStaticSelection predecessor = staticTestSelection.getCommits().get(peassConfig.getMeasurementConfig().getFixedCommitConfig().getCommitOld());
         TestSet ignoredTestsPredecessor;
         if (predecessor != null) {
            ignoredTestsPredecessor = predecessor.getIgnoredAffectedTests();
         } else {
            ignoredTestsPredecessor = new TestSet();
         }

         return new RTSInfos(staticChanges, hasStaticallySelectedTests, ignoredTestsCurrent, ignoredTestsPredecessor, commitSelection != null ? commitSelection.getRemovedTests() : null);
      } else {
         return new RTSInfos(false, false, new TestSet(), new TestSet(), null);
      }
   }
}
