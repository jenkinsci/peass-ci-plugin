package de.dagere.peass.ci;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;

class MeasureVersionBuilderConfigCreationTest {

   @Test
   void testConfigCreation() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setCommitDiff(3);
      builder.setNightlyBuild(false);

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      assertThat(measurementConfig.getFixedCommitConfig().getCommit(), Matchers.equalTo("HEAD"));
      assertThat(measurementConfig.getFixedCommitConfig().getCommitOld(), Matchers.equalTo("HEAD~3"));
   }

   @Test
   void testConfigCreationNightlyAndVersionDiff() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);
      builder.setCommitDiff(2);

      assertThrows(RuntimeException.class,
          builder::getMeasurementConfig);
   }

   @Test
   void testConfigCreationNightly() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setNightlyBuild(true);

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();

      assertThat(measurementConfig.getFixedCommitConfig().getCommit(), Matchers.equalTo("HEAD"));
      assertThat(measurementConfig.getFixedCommitConfig().getCommitOld(), IsNull.nullValue());
   }

   @Test
   void testConfigCreationIncludeError() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass2#*;package.MyClass");

      assertThrows(RuntimeException.class,
          builder::getMeasurementConfig);
   }

   @Test
   void testConfigCreationIncludeRegular() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setIncludes("package.MyClass#*");

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
      assertEquals("package.MyClass#*", measurementConfig.getExecutionConfig().getIncludes().get(0));

   }

   @Test
   void testConfigCreationAnboxTestExecutorError() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setUseAnbox(true);

      assertThrows(RuntimeException.class,
          builder::getMeasurementConfig);
   }

   @Test
   void testConfigCreationAnboxNoGradleTasksError() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();

      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");
      builder.setAndroidGradleTasks("");

      assertThrows(RuntimeException.class,
          builder::getMeasurementConfig);
   }

   @Test
   void testConfigCreationAnboxNoManifestError() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();

      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");
      builder.setAndroidManifest("");

      assertThrows(RuntimeException.class,
          builder::getMeasurementConfig);
   }

   @Test
   void testConfigCreationAnboxDefaultValues() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder();
      builder.setUseAnbox(true);
      builder.setTestExecutor("de.dagere.peass.execution.gradle.AnboxTestExecutor");

      MeasurementConfig measurementConfig = builder.getMeasurementConfig();
     assertNull(measurementConfig.getExecutionConfig().getAndroidCompileSdkVersion());
      assertEquals("", builder.getAndroidCompileSdkVersion());
     assertNull(measurementConfig.getExecutionConfig().getAndroidMinSdkVersion());
      assertEquals("", builder.getAndroidMinSdkVersion());
     assertNull(measurementConfig.getExecutionConfig().getAndroidTargetSdkVersion());
      assertEquals("", builder.getAndroidTargetSdkVersion());
     assertNull(measurementConfig.getExecutionConfig().getAndroidGradleVersion());
      assertEquals("", builder.getAndroidGradleVersion());
     assertNull(measurementConfig.getExecutionConfig().getAndroidTestPackageName());
      assertEquals("", builder.getAndroidTestPackageName());
   }
}
