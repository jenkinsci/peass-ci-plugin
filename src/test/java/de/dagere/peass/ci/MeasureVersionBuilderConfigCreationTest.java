package de.dagere.peass.ci;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.MeasurementConfig;

public class MeasureVersionBuilderConfigCreationTest {

   @Test
   public void testConfigCreation() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setCommitDiff(3);
      builder.setNightlyBuild(false);

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      MatcherAssert.assertThat(measurementConfig.getFixedCommitConfig().getCommit(), Matchers.equalTo("HEAD"));
      MatcherAssert.assertThat(measurementConfig.getFixedCommitConfig().getCommitOld(), Matchers.equalTo("HEAD~3"));
   }

   @Test
   public void testConfigCreationNightlyAndVersionDiff() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);
      builder.setCommitDiff(2);

      Assertions.assertThrows(RuntimeException.class, () -> {
         MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      });
   }

   @Test
   public void testConfigCreationNightly() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      
      MatcherAssert.assertThat(measurementConfig.getFixedCommitConfig().getCommit(), Matchers.equalTo("HEAD"));
      MatcherAssert.assertThat(measurementConfig.getFixedCommitConfig().getCommitOld(), IsNull.nullValue());
   }
   
   @Test
   public void testConfigCreationIncludeError() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass2#*;package.MyClass");
      
      Assert.assertThrows(RuntimeException.class, () -> {
         MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      });
   }
   
   @Test
   public void testConfigCreationIncludeRegular() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass#*");

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      Assert.assertEquals("package.MyClass#*", measurementConfig.getExecutionConfig().getIncludes().get(0));
      
   }

   @Test
   public void testConfigCreationAnboxTestExecutorError() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setUseAnbox(true);

      Assertions.assertThrows(RuntimeException.class, () -> {
         builder.getMeasurementConfig();
      });
   }

   @Test
   public void testConfigCreationAnboxNoGradleTasksError() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();

      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");
      builder.setAndroidGradleTasks("");
      
      Assertions.assertThrows(RuntimeException.class, () -> {
         builder.getMeasurementConfig();
      });
   }

   @Test
   public void testConfigCreationAnboxNoManifestError() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();

      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");
      builder.setAndroidManifest("");
      
      Assertions.assertThrows(RuntimeException.class, () -> {
         builder.getMeasurementConfig();
      });
   }

   @Test
   public void testConfigCreationAnboxDefaultValues() throws JsonParseException, JsonMappingException, IOException {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      Assert.assertEquals(null, measurementConfig.getExecutionConfig().getAndroidCompileSdkVersion());
      Assert.assertEquals("", builder.getAndroidCompileSdkVersion());
      Assert.assertEquals(null, measurementConfig.getExecutionConfig().getAndroidMinSdkVersion());
      Assert.assertEquals("", builder.getAndroidMinSdkVersion());
      Assert.assertEquals(null, measurementConfig.getExecutionConfig().getAndroidTargetSdkVersion());
      Assert.assertEquals("", builder.getAndroidTargetSdkVersion());
      Assert.assertEquals(null, measurementConfig.getExecutionConfig().getAndroidGradleVersion());
      Assert.assertEquals("", builder.getAndroidGradleVersion());
      Assert.assertEquals(null, measurementConfig.getExecutionConfig().getAndroidTestPackageName());
      Assert.assertEquals("", builder.getAndroidTestPackageName());
   }
}
