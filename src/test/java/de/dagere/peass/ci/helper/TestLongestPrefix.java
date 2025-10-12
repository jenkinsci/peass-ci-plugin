package de.dagere.peass.ci.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.rca.RCAVisualizer;

class TestLongestPrefix {

   @Test
   void testLongestPrefix() {
      Set<String> oneTestcase = new HashSet<>();
      oneTestcase.add("de.dagere.testpackage.TestClass#methodA");

      assertEquals("de.dagere.testpackage.", RCAVisualizer.getLongestPrefix(oneTestcase));
   }

   @Test
   void testLongestPrefixSeveralTests() {
      Set<String> severalTestcases = new HashSet<>();
      severalTestcases.add("de.dagere.testpackage.TestClass#methodA");
      severalTestcases.add("de.dagere.testpackage.MyTest#methodB");
      severalTestcases.add("de.dagere.testpackage.SomeStuff#methodC");

      assertEquals("de.dagere.testpackage.", RCAVisualizer.getLongestPrefix(severalTestcases));
   }

   @Test
   void testShorterPrefix() {
      Set<String> severalTestcases = new HashSet<>();
      severalTestcases.add("de.dagere.testpackage.TestClass#methodA");
      severalTestcases.add("de.dagere.MyTest#methodB");
      severalTestcases.add("de.dagere.otherPackage.SomeStuff#methodC");

      assertEquals("de.dagere.", RCAVisualizer.getLongestPrefix(severalTestcases));
   }
}
