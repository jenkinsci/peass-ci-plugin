package de.dagere.peass.ci.logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;

import de.dagere.peass.ci.MeasureVersionBuilder;
import de.dagere.peass.ci.PeassProcessConfiguration;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.ci.logs.RTSLogFileReader;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.traces.TraceWriter;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class RTSLogFileTestUtil {

   static final String COMMIT_OLD = "33ce17c04b5218c25c40137d4d09f40fbb3e4f0f";
   static final String COMMIT = "a23e385264c31def8dcda86c3cf64faa698c62d8";

   static final File localFolder = new File("target/" + MeasureVersionBuilder.PEASS_FOLDER_NAME);
   static final File testFolder = new File(localFolder, "current_peass");
   
   private final TestMethodCall test1;
   private final String projectName;

   public RTSLogFileTestUtil(final TestMethodCall test1, final String projectName) {
      this.test1 = test1;
      this.projectName = projectName;
   }

   public void init(final File source) throws IOException {
      if (localFolder.exists()) {
         FileUtils.deleteDirectory(localFolder);
      }
      if (!localFolder.exists()) {
         localFolder.mkdirs();
      }

      FileUtils.copyDirectory(source, testFolder);

      ResultsFolders folders = initExampleLogFile(projectName);

      initExampleTraceDiffFile(folders);
   }

   private ResultsFolders initExampleLogFile(final String projectName) throws IOException {
      ResultsFolders folders = new ResultsFolders(localFolder, projectName);
      File rtsLogFile = folders.getRTSLogFile(COMMIT, COMMIT_OLD);
      FileUtils.write(rtsLogFile, "This is a rts log test", StandardCharsets.UTF_8);
      return folders;
   }

   private void initExampleTraceDiffFile(final ResultsFolders folders) throws IOException {
      File viewMethodDir = folders.getViewMethodDir(COMMIT_OLD, test1);
      File methodFile = new File(viewMethodDir, TraceWriter.getShortCommit(COMMIT_OLD) + ".txt");
      FileUtils.write(methodFile, "This is a trace generated for rts trace diff", StandardCharsets.UTF_8);
   }

   RTSLogFileReader initializeReader() {
      MeasurementConfig peassDemoConfig = new MeasurementConfig(2, COMMIT, COMMIT_OLD);
      PeassProcessConfiguration peassConfig = new PeassProcessConfiguration(false, peassDemoConfig, null, null, 100, false, false, false, null);           

      VisualizationFolderManager visualizationFolders = Mockito.mock(VisualizationFolderManager.class);
      Mockito.when(visualizationFolders.getPeassFolders()).thenReturn(new PeassFolders(testFolder));
      Mockito.when(visualizationFolders.getResultsFolders()).thenReturn(new ResultsFolders(localFolder, projectName));
      RTSLogFileReader reader = new RTSLogFileReader(visualizationFolders, peassConfig);
      return reader;
   }

}
