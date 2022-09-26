package de.dagere.peass.ci.peassOverview.importer;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class JobImportStarter implements Callable<Void> {
   @Option(names = { "-projectResultsFolder",
         "--projectResultsFolder" }, description = "Path to the projects results folder, containing the rts results in results/, the measurement results in measurement-results/ and the rca-results in rca-results/", required = true)
   protected File projectResultsFolder;

   @Option(names = { "-workspaceFolder", "--workspaceFolder" }, description = "Path to the workspace folder (normally $JENKINS_JOME/workspaces/$PROJECTNAME", required = true)
   protected File workspaceFolder;
   
   @Option(names = { "-url", "--url" }, description = "URL to trigger for rebuild (e.g. http://localhost:8080/jenkins/job/MYJOB/build?token=MYTOKEN&delay=0", required = true)
   protected String url;
   
   @Option(names = { "-authentication", "--authentication" }, description = "Authentication, e.g. myUser:012312 (password should be encrypted)", required = false)
   protected String authentication;
   
   @Option(names = { "-timeout", "--timeout" }, description = "Timeout in seconds, default: 10", required = false)
   protected int timeout = 10;

   public static void main(String[] args) {
      final CommandLine commandLine = new CommandLine(new JobImportStarter());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      OneJobImporter oneJobImporter = new OneJobImporter(projectResultsFolder, workspaceFolder, url, authentication, timeout);
      oneJobImporter.startImport();
      return null;
   }

   
}
