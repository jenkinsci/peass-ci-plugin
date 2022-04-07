package de.peass.ci.process;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.process.RTSInfos;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.ResultsFolders;

public class RTSInfosTest {

   @Test
   public void testRTSInfos() throws StreamReadException, DatabindException, IOException {
      ResultsFolders folders = Mockito.mock(ResultsFolders.class);
      File selectionFile = new File("src/test/resources/noSelectedTest/" + ResultsFolders.STATIC_SELECTION_PREFIX + "demo.json");
      Mockito.when(folders.getStaticTestSelectionFile()).thenReturn(selectionFile);

      PeassProcessConfiguration config = Mockito.mock(PeassProcessConfiguration.class);
      MeasurementConfig measurementConfig = new MeasurementConfig(1);
      measurementConfig.getExecutionConfig().setCommit("15f345835d2a0c85070c9d2ffbbb0f098f68adb5");
      Mockito.when(config.getMeasurementConfig()).thenReturn(measurementConfig);

      RTSInfos infos = RTSInfos.readInfosFromFolders(folders, config);

      Assert.assertFalse(infos.isStaticallySelectedTests());
      Assert.assertTrue(infos.isStaticChanges());
   }
}
