package de.dagere.peass.ci.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.ResultsFolders;

class RTSInfosTest {

   @Test
   void testRTSInfos() throws Exception {
      ResultsFolders folders = Mockito.mock(ResultsFolders.class);
      File selectionFile = new File("src/test/resources/noSelectedTest/" + ResultsFolders.STATIC_SELECTION_PREFIX + "demo.json");
      Mockito.when(folders.getStaticTestSelectionFile()).thenReturn(selectionFile);

      PeassProcessConfiguration config = Mockito.mock(PeassProcessConfiguration.class);
      MeasurementConfig measurementConfig = new MeasurementConfig(1);
      measurementConfig.getFixedCommitConfig().setCommit("15f345835d2a0c85070c9d2ffbbb0f098f68adb5");
      Mockito.when(config.getMeasurementConfig()).thenReturn(measurementConfig);

      RTSInfos infos = RTSInfos.readInfosFromFolders(folders, config);

      assertFalse(infos.isStaticallySelectedTests());
      assertTrue(infos.isStaticChanges());
   }
}
