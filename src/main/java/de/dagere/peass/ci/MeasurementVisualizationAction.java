package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hudson.model.InvisibleAction;

public class MeasurementVisualizationAction extends InvisibleAction {

   private String displayName;
   private final File jsFile;

   public MeasurementVisualizationAction(final String displayName, final File jsFile) {
      this.displayName = displayName;
      this.jsFile = jsFile;
   }

   @Override
   public String getUrlName() {
      return displayName.replace("#", "_");
   }

   public String getCSS() throws IOException {
      InputStream cssStream = MeasurementVisualizationAction.class.getClassLoader().getResourceAsStream("diffview.css");
      String content = IOUtils.toString(cssStream, StandardCharsets.UTF_8);
      return content;
   }

   public String getDataJS() throws IOException {
      final String content = FileUtils.readFileToString(jsFile, StandardCharsets.UTF_8);
      return content;
   }
}
