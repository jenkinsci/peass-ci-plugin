package de.peass.ci;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.ci.helper.HistogramValues;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peran.measurement.analysis.ProjectStatistics;

public class TestMeasureVersionAction {
   
   @Test
   public void testPrefix() {
      final ProjectChanges changes = new ProjectChanges();
      changes.addChange(new TestCase("de.package.ClassA", "method1"), "1", new Change("dummy"));
      changes.addChange(new TestCase("de.package.ClassA", "method2"), "1", new Change("dummy"));
      changes.addChange(new TestCase("de.package.ClassB", "method2"), "1", new Change("dummy"));
      changes.addChange(new TestCase("de.package.otherpackage.ClassC", "method2"), "1", new Change("dummy"));
      
      MeasureVersionAction action = new MeasureVersionAction(new MeasurementConfiguration(5), changes.getVersion("1"), new ProjectStatistics(), new HashMap<String, HistogramValues>());
      
   }
}
