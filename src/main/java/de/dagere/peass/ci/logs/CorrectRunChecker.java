package de.dagere.peass.ci.logs;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.peass.ci.helper.VisualizationFolderManager;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Checks wether a measurement run is correct by checking the XML result files
 * @author reichelt
 *
 */
public class CorrectRunChecker {

   private static final Logger LOG = LogManager.getLogger(CorrectRunChecker.class);

   boolean currentRunning = false;
   boolean predecessorRunning = false;

   public CorrectRunChecker(final TestCase testcase, final int vmId, final MeasurementConfiguration measurementConfig, final VisualizationFolderManager visualizationFolders) {
      File basicResultFolder = visualizationFolders.getResultsFolders().getVersionFullResultsFolder(measurementConfig);
      File detailResultsFolder = new File(basicResultFolder, "measurements");
      
      String pathCurrent = PeassFolders.getRelativeFullResultPath(testcase, measurementConfig.getVersion(), measurementConfig.getVersion(), vmId);
      File resultFileCurrent = new File(detailResultsFolder, pathCurrent); 
      currentRunning = checkIsRunning(vmId, resultFileCurrent);

      String pathPredecessor = PeassFolders.getRelativeFullResultPath(testcase, measurementConfig.getVersion(), measurementConfig.getVersionOld(), vmId);
      File resultFilePredecessor = new File(detailResultsFolder, pathPredecessor); 
      predecessorRunning = checkIsRunning(vmId, resultFilePredecessor);
   }

   private boolean checkIsRunning(final int vmId, final File resultFileCurrent) {
      boolean isRunning = false;
      if (resultFileCurrent.exists()) {
         try {
            LOG.debug("Checking: {} - {} ", vmId, resultFileCurrent.getAbsolutePath());
            Kopemedata data = XMLDataLoader.loadData(resultFileCurrent);
            Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
            if (datacollector.getResult().get(0) != null) {
               isRunning = true;
               LOG.debug("File and result are existing - success");
            }
         } catch (JAXBException e) {
            e.printStackTrace();
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
