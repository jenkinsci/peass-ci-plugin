package de.peass.ci;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.helper.HistogramValues;
import de.peass.measurement.analysis.ProjectStatistics;

public class TestMeasureVersionAction {

   @Test
   public void testPrefix() {
      final ProjectChanges changes = getChanges();

      MeasureVersionAction action = new MeasureVersionAction(new MeasurementConfiguration(5), changes.getVersion("1"), new ProjectStatistics(),
            new HashMap<String, HistogramValues>());

      Assert.assertEquals("ClassA", action.getReducedName("de.package.ClassA"));
      Assert.assertEquals("otherpackage.ClassC", action.getReducedName("de.package.otherpackage.ClassC"));
   }

   private ProjectChanges getChanges() {
      final ProjectChanges changes = new ProjectChanges();
      changes.addChange(new TestCase("de.package.ClassA", "method1"), "1", new Change("dummy", "method1"));
      changes.addChange(new TestCase("de.package.ClassA", "method2"), "1", new Change("dummy", "method2"));
      changes.addChange(new TestCase("de.package.ClassB", "method2"), "1", new Change("dummy", "method2"));
      changes.addChange(new TestCase("de.package.otherpackage.ClassC", "method2"), "1", new Change("dummy", "method2"));
      return changes;
   }

   @Test
   public void testIsChanged() {
      final ProjectChanges changes = getChanges();

      MeasureVersionAction action = new MeasureVersionAction(new MeasurementConfiguration(5), changes.getVersion("1"), new ProjectStatistics(),
            new HashMap<String, HistogramValues>());
      
      Assert.assertTrue(action.testIsChanged("de.package.ClassA#method1"));
      Assert.assertTrue(action.testIsChanged("de.package.ClassA#method2"));
      Assert.assertTrue(action.testIsChanged("de.package.ClassB#method2"));
   }
}
