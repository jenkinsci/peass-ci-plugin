package de.peass.ci;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.ci.MeasureVersionAction;
import de.dagere.peass.ci.helper.HistogramValues;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestMeasureVersionAction {

   @Test
   public void testPrefix() {
      final ProjectChanges changes = getChanges();

      HashMap<String, HistogramValues> measurements = new HashMap<String, HistogramValues>();
      measurements.put("de.package.ClassA#method1", null);
      measurements.put("de.package.ClassA#method2", null);
      measurements.put("de.package.ClassB#method2", null);
      measurements.put("de.package.otherpackage.ClassC#method2", null);

      MeasureVersionAction action = new MeasureVersionAction(new MeasurementConfig(5), changes.getVersion("1"), new ProjectStatistics(),
            new HashMap<>(), measurements, new HashMap<>());

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

      MeasureVersionAction action = new MeasureVersionAction(new MeasurementConfig(5), changes.getVersion("1"), new ProjectStatistics(),
            new HashMap<>(), new HashMap<>(), new HashMap<>());

      Assert.assertTrue(action.testIsChanged("de.package.ClassA#method1"));
      Assert.assertTrue(action.testIsChanged("de.package.ClassA#method2"));
      Assert.assertTrue(action.testIsChanged("de.package.ClassB#method2"));
   }
}
