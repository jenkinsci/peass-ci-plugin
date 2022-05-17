package de.dagere.peass.ci.peassAnalysis;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import hudson.model.TaskListener;

public class TestPeassAnalysisBuilder {
   
   @Test
   public void testOnlyStaticChange() throws StreamReadException, DatabindException, IOException {
      File folder = new File("src/test/resources/peassAnalysis");
      
      PeassAnalysisAction action = PeassAnalysisBuilder.createAction(Mockito.mock(TaskListener.class), folder, "demo");
      
      Assert.assertEquals(1, action.getChanges().getChangeCount());
      Assert.assertEquals(1, action.getChangeLines().size());
      
   }
}
