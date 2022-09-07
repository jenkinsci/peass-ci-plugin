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

   public static void main(String[] args) {
      final CommandLine commandLine = new CommandLine(new JobImportStarter());
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      OneJobImporter oneJobImporter = new OneJobImporter(projectResultsFolder, workspaceFolder);
      oneJobImporter.startImport();
      return null;
   }

   
}
