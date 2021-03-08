package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class RCAVisualizationAction implements RunAction2 {

   private transient Run<?, ?> run;
   private String displayName;
   private final File jsFile;
   
   public RCAVisualizationAction(final String displayName, final File jsFile) {
      this.displayName = displayName;
      this.jsFile = jsFile;
   }
   
   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/trend.png";
   }

   @Override
   public String getDisplayName() {
      return displayName;
   }

   @Override
   public String getUrlName() {
      return displayName.replace("#", "_");
   }
   
   public String getCSS() throws IOException {
      InputStream cssStream = RCAVisualizationAction.class.getClassLoader().getResourceAsStream("diffview.css");
      String content = IOUtils.toString(cssStream, StandardCharsets.UTF_8);
      return content;
   }
   
   public String getDataJS() throws IOException {
      final String content = FileUtils.readFileToString(jsFile, StandardCharsets.UTF_8);
      return content;
   }
   
   @Override
   public void onAttached(final Run<?, ?> run) {
      this.run = run;
   }

   @Override
   public void onLoad(final Run<?, ?> run) {
      this.run = run;
   }
   
   public Run<?, ?> getRun() {
      return run;
   }

}
