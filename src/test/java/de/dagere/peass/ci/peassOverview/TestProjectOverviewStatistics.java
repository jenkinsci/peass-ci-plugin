package de.dagere.peass.ci.peassOverview;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.peassOverview.classification.ClassifiedProject;
import de.dagere.peass.ci.peassOverview.classification.TestcaseClassification;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.StaticTestSelection;

public class TestProjectOverviewStatistics {

   private static final String VERSION_4 = "000004";
   private static final String VERSION_3 = "000003";
   private static final String VERSION_2 = "000002";
   private static final String VERSION_1 = "000001";
   private static final String EXAMPLE_CATEGORY_OPTIMIZATION = "optimization";
   private static final String EXAMPLE_CATEGORY_FUNCTIONAL = "functional";
   private static final String EXAMPLE_CATEGORY_UPDATE = "update";
   private static final String EXAMPLE_CATEGORY_REMOVE_CALL = "remoteCall";

   @Test
   public void testCounting() {
      StaticTestSelection selection = new StaticTestSelection();
      ProjectChanges changes = buildChanges();
      ProjectData data = new ProjectData(selection, changes, new ProjectStatistics(), false);

      ClassifiedProject classifiedProject = buildClassification();

      ProjectOverviewStatistic overviewStatistic = ProjectOverviewStatistic.getFromClassification("Test", data, classifiedProject);

      Assert.assertEquals(3, overviewStatistic.getCategoryCommitCount().get(EXAMPLE_CATEGORY_FUNCTIONAL).intValue());
      Assert.assertEquals(6, overviewStatistic.getCategoryTestCount().get(EXAMPLE_CATEGORY_FUNCTIONAL).intValue());
      Assert.assertEquals(1, overviewStatistic.getCategoryCommitCount().get(EXAMPLE_CATEGORY_OPTIMIZATION).intValue());
      Assert.assertEquals(1, overviewStatistic.getCategoryCommitCount().get(EXAMPLE_CATEGORY_UPDATE).intValue());

      Assert.assertEquals(2, overviewStatistic.getCategoryTestCount().get(EXAMPLE_CATEGORY_REMOVE_CALL).intValue());
      Assert.assertEquals(1, overviewStatistic.getCategoryCommitCount().get(EXAMPLE_CATEGORY_REMOVE_CALL).intValue());

      Assert.assertEquals(3, overviewStatistic.getCommitsWithChange());
   }

   private ClassifiedProject buildClassification() {
      ClassifiedProject classifiedProject = new ClassifiedProject();

      TestcaseClassification testcaseClassificationCommit1 = classifiedProject.getClassification(VERSION_1);
      testcaseClassificationCommit1.setClassificationValue("TestA#test", EXAMPLE_CATEGORY_FUNCTIONAL);
      testcaseClassificationCommit1.setClassificationValue("TestB#test", EXAMPLE_CATEGORY_FUNCTIONAL);
      testcaseClassificationCommit1.setClassificationValue("TestC#test", EXAMPLE_CATEGORY_OPTIMIZATION);

      TestcaseClassification testcaseClassificationCommit2 = classifiedProject.getClassification(VERSION_2);
      testcaseClassificationCommit2.setClassificationValue("TestA#test", EXAMPLE_CATEGORY_FUNCTIONAL);
      testcaseClassificationCommit2.setClassificationValue("TestB#test", EXAMPLE_CATEGORY_FUNCTIONAL);
      testcaseClassificationCommit2.setClassificationValue("TestC#test", EXAMPLE_CATEGORY_UPDATE);

      TestcaseClassification testcaseClassificationCommit3 = classifiedProject.getClassification(VERSION_3);
      testcaseClassificationCommit3.setClassificationValue("TestA#test", EXAMPLE_CATEGORY_FUNCTIONAL);
      testcaseClassificationCommit3.setClassificationValue("TestB#test", EXAMPLE_CATEGORY_FUNCTIONAL);

      TestcaseClassification testcaseUnmeasuredClassificationCommit4 = classifiedProject.getUnmeasuredClassification(VERSION_4);
      testcaseUnmeasuredClassificationCommit4.setClassificationValue("TestD#test", EXAMPLE_CATEGORY_REMOVE_CALL);
      testcaseUnmeasuredClassificationCommit4.setClassificationValue("TestE#test", EXAMPLE_CATEGORY_REMOVE_CALL);
      return classifiedProject;
   }

   private ProjectChanges buildChanges() {
      ProjectChanges changes = new ProjectChanges();
      changes.addChange(new TestMethodCall("TestA", "test"), VERSION_1, new Change());
      changes.addChange(new TestMethodCall("TestB", "test"), VERSION_1, new Change());
      changes.addChange(new TestMethodCall("TestC", "test"), VERSION_1, new Change());

      changes.addChange(new TestMethodCall("TestA", "test"), VERSION_2, new Change());
      changes.addChange(new TestMethodCall("TestB", "test"), VERSION_2, new Change());
      changes.addChange(new TestMethodCall("TestC", "test"), VERSION_2, new Change());

      changes.addChange(new TestMethodCall("TestA", "test"), VERSION_3, new Change());
      changes.addChange(new TestMethodCall("TestB", "test"), VERSION_3, new Change());
      return changes;
   }
}
