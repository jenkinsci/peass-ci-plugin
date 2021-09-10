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

      Assert.assertEquals("de.dagere.testpackage", RCAVisualizer.getLongestPrefix(oneTestcase));
   }
}
