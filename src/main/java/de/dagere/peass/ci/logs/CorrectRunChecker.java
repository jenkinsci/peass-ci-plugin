package de.dagere.peass.ci.logs;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;

/**
 * Checks wether a measurement run is correct by checking the XML result files
 * 
 * @author reichelt
 *
 */
public class CorrectRunChecker {

   private static final Logger LOG = LogManager.getLogger(CorrectRunChecker.class);

   boolean currentRunning = false;
   boolean predecessorRunning = false;

   public CorrectRunChecker(final TestCase testcase, final int vmId, final MeasurementConfig measurementConfig, final VisualizationFolderManager visualizationFolders) {
      File basicResultFolder = visualizationFolders.getResultsFolders().getVersionFullResultsFolder(measurementConfig);
      File detailResultsFolder = new File(basicResultFolder, "measurements");

      String pathCurrent = PeassFolders.getRelativeFullResultPath(testcase, measurementConfig.getExecutionConfig().getCommit(), measurementConfig.getExecutionConfig().getCommit(),
            vmId);
      File resultFileCurrent = new File(detailResultsFolder, pathCurrent);
      currentRunning = checkIsRunning(vmId, resultFileCurrent);

      String pathPredecessor = PeassFolders.getRelativeFullResultPath(testcase, measurementConfig.getExecutionConfig().getCommit(),
            measurementConfig.getExecutionConfig().getCommitOld(), vmId);
      File resultFilePredecessor = new File(detailResultsFolder, pathPredecessor);
      predecessorRunning = checkIsRunning(vmId, resultFilePredecessor);
   }

   private boolean checkIsRunning(final int vmId, final File resultFileCurrent) {
      boolean isRunning = false;
      if (resultFileCurrent.exists()) {
         LOG.debug("Checking: {} - {} ", vmId, resultFileCurrent.getAbsolutePath());
         Kopemedata data = JSONDataLoader.loadData(resultFileCurrent);
         DatacollectorResult datacollector = data.getFirstTimeDataCollector();
         if (datacollector.getResults().get(0) != null) {
            isRunning = true;
            LOG.debug("File and result are existing - success");
         }
      } else {
         LOG.debug("File {} missing", resultFileCurrent);
      }
      return isRunning;
   }

   public boolean isCurrentRunning() {
      return currentRunning;
   }

   public boolean isPredecessorRunning() {
      return predecessorRunning;
   }

}
