package de.dagere.peass.ci.clean.callables;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.folders.PeassFolders;

public class CleanUtilTest {

   private static final String PROJECT_NAME = "demo-project-gradle_7_3_3-java17";
   private static final String PROJECTNAME_FULL_PEASS  = PROJECT_NAME + PeassFolders.PEASS_FULL_POSTFIX;
   private static final File RESOURCES = TestConstants.TEST_RESOURCES;
   private static final File FULL_PEASS_FILE = new File(RESOURCES, PROJECTNAME_FULL_PEASS);

   private static final File CURRENT_FOLDER = TestConstants.CURRENT_FOLDER;
   private static final File FULL_PEASS_COPY = new File(CURRENT_FOLDER, PROJECTNAME_FULL_PEASS);

   @Test
   public void testCleanProjectFolder() throws IOException {

      TestUtil.deleteContents(CURRENT_FOLDER);
      FileUtils.copyToDirectory(FULL_PEASS_FILE, CURRENT_FOLDER);

      CleanUtil.cleanProjectFolder(CURRENT_FOLDER, PROJECTNAME_FULL_PEASS);
      assertFalse(FULL_PEASS_COPY.exists());
   }

}
