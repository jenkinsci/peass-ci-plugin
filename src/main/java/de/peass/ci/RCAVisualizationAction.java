package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class RCAVisualizationAction implements RunAction2 {

   private String displayName;
   private final File htmlFile;
   private transient Run run;
   
   public RCAVisualizationAction(String displayName, File htmlFile) {
      this.displayName = displayName;
      this.htmlFile = htmlFile;
   }
   
   @Override
   public String getIconFileName() {
      return "document.png";
   }

   @Override
   public String getDisplayName() {
      return displayName;
   }

   @Override
   public String getUrlName() {
      return displayName.replace("#", "_");
   }
   
   public File getHtmlFile() {
      return htmlFile;
   }
   
   public String readFile(File file) throws IOException {
      final String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      return content;
   }
   
   @Override
   public void onAttached(Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(Run<?, ?> run) {
      this.run = run;
   }

}
