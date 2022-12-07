package de.dagere.peass.ci.process;

import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestIncludeExcludeParser {

   @Test
   public void testRegularPattern() {
      String example = "* de.dagere.peass.ClazzA.methodA();public void de.dagere.peass.ClazzB.methodB(int);";
      Set<String> strings = IncludeExcludeParser.getStringSet(example);
      MatcherAssert.assertThat(strings, IsIterableWithSize.iterableWithSize(2));
   }

   @Test
   public void testWrongPattern() {
      String example = "* de.dagere.peass.ClazzA.methodA();public void de.dagere.peass.ClazzB#methodB(int);";

      Assert.assertThrows(RuntimeException.class,
            () -> {
               IncludeExcludeParser.getStringSet(example);
            });
   }
}
