package de.dagere.peass.ci;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class RCAVisualizationAction extends VisibleAction {

   private String displayName;
   private final String jsData;
   private final String predecessorCommitTreeJS, currentCommitTreeJS;
   
   public RCAVisualizationAction(int id, final String displayName, final String jsData, String predecessorCommitTreeJS, String currentCommitTreeJS) {
      super(id);
      this.displayName = displayName;
      this.jsData = jsData;
      this.predecessorCommitTreeJS = predecessorCommitTreeJS;
      this.currentCommitTreeJS = currentCommitTreeJS;
   }
   
   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/rca.png";
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
   
   public String getDataJS() {
      return jsData;
   }
   
   public String getPredecessorCommitTreeJS() {
      return predecessorCommitTreeJS;
   }
   
   public String getCurrentCommitTreeJS() {
      return currentCommitTreeJS;
   }
}
