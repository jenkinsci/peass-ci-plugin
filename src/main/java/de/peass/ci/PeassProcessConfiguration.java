package de.peass.ci;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.execution.EnvironmentVariables;

public class PeassProcessConfiguration {
   private final boolean updateSnapshotDependencies;
   private final MeasurementConfiguration measurementConfig;
   private final DependencyConfig dependencyConfig;
   private final EnvironmentVariables envVars;

   public PeassProcessConfiguration(final boolean updateSnapshotDependencies, final MeasurementConfiguration measurementConfig, final DependencyConfig dependencyConfig,
         final EnvironmentVariables envVars) {
      this.updateSnapshotDependencies = updateSnapshotDependencies;
      this.measurementConfig = measurementConfig;
      this.dependencyConfig = dependencyConfig;
      this.envVars = envVars;
   }

   public boolean isUpdateSnapshotDependencies() {
      return updateSnapshotDependencies;
   }

   public MeasurementConfiguration getMeasurementConfig() {
      return measurementConfig;
   }

   public DependencyConfig getDependencyConfig() {
      return dependencyConfig;
   }

   public EnvironmentVariables getEnvVars() {
      return envVars;
   }

}
