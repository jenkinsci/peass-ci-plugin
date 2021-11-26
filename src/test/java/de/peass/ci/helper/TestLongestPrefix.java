package de.peass.ci.helper;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.helper.RCAVisualizer;

public class TestLongestPrefix {

   @Test
   public void testLongestPrefix() {
      Set<String> oneTestcase = new HashSet<>();
      oneTestcase.add("de.dagere.testpackage.TestClass#methodA");

      Assert.assertEquals("de.dagere.testpackage.", RCAVisualizer.getLongestPrefix(oneTestcase));
   }
   
   @Test
   public void testLongestPrefixSeveralTests() {
      Set<String> severalTestcases = new HashSet<>();
      severalTestcases.add("de.dagere.testpackage.TestClass#methodA");
      severalTestcases.add("de.dagere.testpackage.MyTest#methodB");
      severalTestcases.add("de.dagere.testpackage.SomeStuff#methodC");

      Assert.assertEquals("de.dagere.testpackage.", RCAVisualizer.getLongestPrefix(severalTestcases));
   }
   
   @Test
   public void testShorterPrefix() {
      Set<String> severalTestcases = new HashSet<>();
      severalTestcases.add("de.dagere.testpackage.TestClass#methodA");
      severalTestcases.add("de.dagere.MyTest#methodB");
      severalTestcases.add("de.dagere.otherPackage.SomeStuff#methodC");

      Assert.assertEquals("de.dagere.", RCAVisualizer.getLongestPrefix(severalTestcases));
   }
}
