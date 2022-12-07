package de.dagere.peass.ci;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import de.dagere.peass.vcs.GitCommit;

public class RCAVisualizationAction extends VisibleAction {

   private String displayName;
   private final String jsData;
   private final String predecessorCommitTreeJS, currentCommitTreeJS;
   private final GitCommit commit;
   private final String testcase;
   
   public RCAVisualizationAction(int id, final String displayName, final String jsData, String predecessorCommitTreeJS, String currentCommitTreeJS, GitCommit commit, String testcase) {
      super(id);
      this.displayName = displayName;
      this.jsData = jsData;
      this.predecessorCommitTreeJS = predecessorCommitTreeJS;
      this.currentCommitTreeJS = currentCommitTreeJS;
      this.commit = commit;
      this.testcase = testcase;
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
   
   public String getCommit() { 
      return commit != null ? commit.getTag() : null;
   }
   
   public String getTestcase() {
      return testcase;
   }
   
   public String getComitter() {
      return commit != null ? commit.getComitter() : null;
   }
}
