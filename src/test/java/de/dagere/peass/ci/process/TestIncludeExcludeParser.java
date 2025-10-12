package de.dagere.peass.ci.process;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.jupiter.api.Test;

class TestIncludeExcludeParser {

   @Test
   void testRegularPattern() {
      String example = "* de.dagere.peass.ClazzA.methodA();public void de.dagere.peass.ClazzB.methodB(int);";
      Set<String> strings = IncludeExcludeParser.getStringSet(example);
      assertThat(strings, IsIterableWithSize.iterableWithSize(2));
   }

   @Test
   void testWrongPattern() {
      String example = "* de.dagere.peass.ClazzA.methodA();public void de.dagere.peass.ClazzB#methodB(int);";

      assertThrows(RuntimeException.class,
            () -> IncludeExcludeParser.getStringSet(example));
   }
}
