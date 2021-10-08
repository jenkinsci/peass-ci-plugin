package de.peass.ci;

import java.io.File;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.config.MeasurementConfiguration;

public class MeasureVersionBuilderConfigCreationTest {

   private static final File CURRENT_FOLDER = new File("target/current");

   @Test
   public void testConfigCreation() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setVersionDiff(3);
      builder.setNightlyBuild(false);

      MeasurementConfiguration measurementConfig = builder.getMeasurementConfig();
      MatcherAssert.assertThat(measurementConfig.getExecutionConfig().getVersion(), Matchers.equalTo("HEAD"));
      MatcherAssert.assertThat(measurementConfig.getExecutionConfig().getVersionOld(), Matchers.equalTo("HEAD~3"));
   }

   @Test
   public void testConfigCreationNightlyAndVersionDiff() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);
      builder.setVersionDiff(2);

      Assertions.assertThrows(RuntimeException.class, () -> {
         MeasurementConfiguration measurementConfig = builder.getMeasurementConfig();
      });
   }

   @Test
   public void testConfigCreationNightly() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);

      MeasurementConfiguration measurementConfig = builder.getMeasurementConfig();
      
      MatcherAssert.assertThat(measurementConfig.getExecutionConfig().getVersion(), Matchers.equalTo("HEAD"));
      MatcherAssert.assertThat(measurementConfig.getExecutionConfig().getVersionOld(), IsNull.nullValue());
   }
   
   @Test
   public void testConfigCreationIncludeError() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass2#*;package.MyClass");
      
      Assert.assertThrows(RuntimeException.class, () -> {
         MeasurementConfiguration measurementConfig = builder.getMeasurementConfig();
      });
   }
   
   @Test
   public void testConfigCreationIncludeRegular() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass#*");

      MeasurementConfiguration measurementConfig = builder.getMeasurementConfig();
      Assert.assertEquals("package.MyClass#*", measurementConfig.getIncludes().get(0));
      
   }
}
