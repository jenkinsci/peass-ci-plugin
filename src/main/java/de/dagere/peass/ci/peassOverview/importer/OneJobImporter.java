package de.dagere.peass.ci.peassOverview.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.ci.rca.RCAExecutor;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitList;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;

public class OneJobImporter {
   
   private static final Logger LOG = LogManager.getLogger(OneJobImporter.class);
   
   private final StaticTestSelection staticSelection;
   private final ExecutionData executionData;
   private final ProjectChanges projectChanges;

   private final File workspaceFolder;
   private final File projectResultsFolder;
   private final File fullPeassFolder;

   private final String projectName;

   private final String url;
   private final String authentication;

   private final CommitList commits = new CommitList();

   public OneJobImporter(File projectResultsFolder, File workspaceFolder, String url, String authentication) throws StreamReadException, DatabindException, IOException {
      this.projectResultsFolder = projectResultsFolder;
      this.workspaceFolder = workspaceFolder;
      this.url = url;
      this.authentication = authentication;
      fullPeassFolder = new File(workspaceFolder.getParentFile(), workspaceFolder.getName() + "_fullPeass");

      projectName = projectResultsFolder.getName();

      File staticSelectionFile = new File(projectResultsFolder, "results/staticTestSelection_" + projectName + ".json");
      File executionFile = new File(projectResultsFolder, "results/traceTestSelection_" + projectName + ".json");

      staticSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
      executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
      projectChanges = Constants.OBJECTMAPPER.readValue(new File(projectResultsFolder, "measurement-results/changes.json"), ProjectChanges.class);

      File jenkinsPropertyFolder = new File(fullPeassFolder, "properties_" + projectName);
      File resultsPropertyFolder = new File(projectResultsFolder, "results/properties_" + projectName);
      FileUtils.copyDirectory(resultsPropertyFolder, jenkinsPropertyFolder);

      for (String commitName : executionData.getCommitNames()) {
         final GitCommit gc = new GitCommit(commitName, "", "", "");
         commits.getCommits().add(gc);
      }
      Constants.OBJECTMAPPER.writeValue(new File(fullPeassFolder, "commits.json"), commits);
   }

   public void startImport() throws StreamWriteException, DatabindException, IOException, InterruptedException {
      StaticTestSelection copiedStaticSelection = new StaticTestSelection();
      copiedStaticSelection.setInitialcommit(staticSelection.getInitialcommit());
      ExecutionData copiedSelection = new ExecutionData();

      LOG.info("Importing " + executionData.getCommits().size() +  " commits");
      
      for (Entry<String, TestSet> commitSelection : executionData.getCommits().entrySet()) {
         String commit = commitSelection.getKey();
         String predecessor = commitSelection.getValue().getPredecessor();
         Changes changes = projectChanges.getCommitChanges(commit);
         
         LOG.debug("Importing {}, Changes: {}", commit, changes.getTestcaseChanges().size());
         
         copiedSelection.addCall(commit, commitSelection.getValue());
         CommitStaticSelection commitStaticSelection = staticSelection.getCommits().get(commit);
         copiedStaticSelection.getCommits().put(commit, commitStaticSelection);

         Constants.OBJECTMAPPER.writeValue(new File(fullPeassFolder, "traceTestSelection_" + projectName + ".json"), copiedSelection);
         Constants.OBJECTMAPPER.writeValue(new File(fullPeassFolder, "staticTestSelection_" + projectName + ".json"), copiedStaticSelection);
         if (changes != null && !changes.getTestcaseChanges().isEmpty()) {
            prepareData(commit, predecessor);

            triggerBuild(projectName);

            Thread.sleep(10000);
         }
      }
      
      triggerBuild(projectName);
   }

   private void prepareData(String commit, String predecessor)
         throws IOException, StreamReadException, DatabindException, StreamWriteException {
      File fakeMeasurementFolder = new File(fullPeassFolder, "measurement_" + commit + "_" + predecessor);
      if (!fakeMeasurementFolder.mkdir() && !fakeMeasurementFolder.exists()) {
         throw new RuntimeException("Could not create " + fakeMeasurementFolder);
      }

      GitUtils.goToCommit(commit, workspaceFolder);

      importRCAData(commit);

      importMeasurementFolder(commit, predecessor, fakeMeasurementFolder);

     
   }

   private void importRCAData(String commit) throws IOException {
      File jobCommitFolder = new File(fullPeassFolder, projectName + "_peass/rca/treeMeasurementResults/" + commit);
      if (!jobCommitFolder.mkdirs() && !jobCommitFolder.exists()) {
         throw new RuntimeException("Could not create " + jobCommitFolder);
      }
      File rcaCommitFolder = new File(projectResultsFolder, "rca-results/treeMeasurementResults/" + commit);
      File[] clazzFolders = rcaCommitFolder.listFiles();
      if (clazzFolders != null) {
         for (File clazzFolder : clazzFolders) {
            File jobClazzFolder = new File(jobCommitFolder, clazzFolder.getName());
            FileUtils.copyDirectory(clazzFolder, jobClazzFolder);
         }
      }
   }

   private void importMeasurementFolder(String commit, String predecessor, File fakeMeasurementFolder)
         throws IOException, StreamReadException, DatabindException, StreamWriteException {
      File measurementsFullFolder = new File(projectResultsFolder, "measurement-results/measurementsFull");
      File[] jsonFiles = measurementsFullFolder.listFiles();
      if (jsonFiles != null) {
         for (File jsonFile : jsonFiles) {
            if (jsonFile.getName().endsWith(".json")) {
               Kopemedata data = Constants.OBJECTMAPPER.readValue(jsonFile, Kopemedata.class);
               for (VMResultChunk chunk : data.getChunks()) {
                  Set<String> commits = new HashSet<>();
                  for (VMResult result : chunk.getResults()) {
                     commits.add(result.getCommit());
                  }
                  if (commits.size() == 2 && commits.contains(commit) && commits.contains(predecessor)) {
                     String clazzName = data.getClazz();
                     Kopemedata copiedData = new Kopemedata(clazzName);
                     copiedData.getMethods().add(new TestMethod(data.getFirstMethodResult().getMethod()));
                     copiedData.getFirstMethodResult().getDatacollectorResults().add(new DatacollectorResult(data.getFirstTimeDataCollector().getName()));
                     copiedData.getChunks().add(chunk);
                     File resultFile = new File(fakeMeasurementFolder, jsonFile.getName());
                     Constants.OBJECTMAPPER.writeValue(resultFile, copiedData);
                  }
               }
            }
         }
      }
   }

   private void triggerBuild(String projectName) throws MalformedURLException, IOException, UnsupportedEncodingException {
      URL urlObject = new URL(url);
      URLConnection uc = urlObject.openConnection();
      
      if (authentication != null) {
         String userpass = authentication;
         String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes("UTF-8")), "UTF-8");
         
         uc.setRequestProperty("Authorization", basicAuth);
      }
      
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"))) {
         for (String line; (line = reader.readLine()) != null;) {
            
            System.out.println(line);
         }
      }
   }
}
