package de.dagere.peass.ci.process;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.VersionStaticSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class RTSInfos {
   private final boolean staticChanges;
   private final boolean staticallySelectedTests;

   public RTSInfos(final boolean staticChanges, final boolean staticallySelectedTests) {
      this.staticChanges = staticChanges;
      this.staticallySelectedTests = staticallySelectedTests;
   }

   public boolean isStaticChanges() {
      return staticChanges;
   }

   public boolean isStaticallySelectedTests() {
      return staticallySelectedTests;
   }

   public static RTSInfos readInfosFromFolders(final ResultsFolders results, final PeassProcessConfiguration peassConfig) throws StreamReadException, DatabindException, IOException {
      File dependencyFile = results.getDependencyFile();
      if (dependencyFile.exists()) {
         boolean staticChanges = false;
         Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionStaticSelection version = dependencies.getVersions().get(peassConfig.getMeasurementConfig().getExecutionConfig().getVersion());
         boolean hasStaticallySelectedTests = false;
         if (version != null) {
            if (!version.getChangedClazzes().isEmpty()) {
               staticChanges = true;
            }
            TestSet tests = version.getTests();
            hasStaticallySelectedTests = !tests.getTests().isEmpty();
         }
         return new RTSInfos(staticChanges, hasStaticallySelectedTests);
      } else {
         return new RTSInfos(false, false);
      }
   }
}
