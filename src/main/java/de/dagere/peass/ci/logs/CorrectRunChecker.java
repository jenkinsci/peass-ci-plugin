package de.dagere.peass.ci.logs;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.PeassFolders;

/**
 * Checks wether a measurement run is correct by checking the XML result files
 *
 * @author reichelt
 */
public class CorrectRunChecker {

   private static final Logger LOG = LogManager.getLogger(CorrectRunChecker.class);

   boolean currentRunning = false;
   boolean predecessorRunning = false;

   public CorrectRunChecker(final TestMethodCall testcase, final int vmId, final MeasurementConfig measurementConfig, final VisualizationFolderManager visualizationFolders) {
      File basicResultFolder = visualizationFolders.getResultsFolders().getCommitFullResultsFolder(measurementConfig);
      File detailResultsFolder = new File(basicResultFolder, "measurements");

      FixedCommitConfig fixedCommitConfig = measurementConfig.getFixedCommitConfig();
      String pathCurrent = PeassFolders.getRelativeFullResultPath(testcase, fixedCommitConfig.getCommit(), fixedCommitConfig.getCommit(),
            vmId);
      File resultFileCurrent = new File(detailResultsFolder, pathCurrent);
      currentRunning = checkIsRunning(vmId, resultFileCurrent);

      String pathPredecessor = PeassFolders.getRelativeFullResultPath(testcase, fixedCommitConfig.getCommit(),
            fixedCommitConfig.getCommitOld(), vmId);
      File resultFilePredecessor = new File(detailResultsFolder, pathPredecessor);
      predecessorRunning = checkIsRunning(vmId, resultFilePredecessor);
   }

   private boolean checkIsRunning(final int vmId, final File resultFile) {
      final boolean isRunning = true;
      if (resultFile.exists()) {
         LOG.debug("Checking: {} - {} ", vmId, resultFile.getAbsolutePath());
         Kopemedata data = JSONDataLoader.loadData(resultFile);
         List<VMResult> vmResults = data.getMethods().get(0).getDatacollectorResults().get(0).getResults();
         for (VMResult vmResult : vmResults) {
            if (anyErrorOccurredInVM(vmResult)) {
               return !isRunning;
            }
         }
      } else {
         LOG.debug("File {} missing", resultFile);
         return !isRunning;
      }
      return isRunning;
   }

   private boolean anyErrorOccurredInVM(final VMResult vmResult) {
      return (vmResult.isError() || vmResult.isFailure() || vmResult.isSubthreadTimeout());
   }

   public boolean isCurrentRunning() {
      return currentRunning;
   }

   public boolean isPredecessorRunning() {
      return predecessorRunning;
   }

}
