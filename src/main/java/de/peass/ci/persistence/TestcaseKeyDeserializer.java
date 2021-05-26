package de.peass.ci.persistence;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestcaseKeyDeserializer extends KeyDeserializer {

   @Override
   public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
      String[] splitted = key.split(" ");
      String clazz = splitted[1].substring("[clazz=".length(), splitted[1].length() - 1);
      String method = splitted[2].substring("method=".length(), splitted[2].length() - 1);
      if (splitted.length > 3) {
         String module = splitted[3].substring("module=".length(), splitted[2].length() - 1);
         return new TestCase(clazz, method, module);
      } else {
         return new TestCase(clazz, method);
      }
   }

}