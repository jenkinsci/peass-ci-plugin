package de.dagere.peass.ci.peassOverview;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.LinkedList;
import java.util.Map;

import org.hamcrest.collection.IsIterableWithSize;
import org.hamcrest.core.IsIterableContaining;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import hudson.model.Run;
import hudson.model.TaskListener;

class TestProjectDataCreation {

   private static final String SIMPLE_CREATION_PROJECT = "simpleCreation";
   private static final String DAY_ON_MONTH_BORDER_PROJECT = "dayOnMonthBorderCreation";

   private final File virtualProjectFolder = new File("src/test/resources/peassOverview/virtualProjectFolder/1");

   @BeforeEach
   void setUp() {
      virtualProjectFolder.mkdirs();
   }

   @Test
   void testFindInSimpleCreation() {
      Map<String, ProjectData> projectData = mockAndCreateProjectData(SIMPLE_CREATION_PROJECT, new DateTime(2022, 3, 21, 17, 22), PeassOverviewBuilder.LAST_DAY);

      assertThat(projectData.keySet(), IsIterableContaining.hasItem(SIMPLE_CREATION_PROJECT));
      ProjectData simpleCreationProjectData = projectData.get(SIMPLE_CREATION_PROJECT);

      assertEquals("6ce9d6a3154c4ce8f617c357cf466fab222d27ef", simpleCreationProjectData.getChangeLines().get(0).getCommit());
   }

   @Test
   void testTooLateInSimpleCreation() {
      Map<String, ProjectData> projectData = mockAndCreateProjectData(SIMPLE_CREATION_PROJECT, new DateTime(2022, 3, 23, 17, 22), PeassOverviewBuilder.LAST_DAY);

      assertThat(projectData.keySet(), IsIterableContaining.hasItem(SIMPLE_CREATION_PROJECT));
      ProjectData simpleCreationProjectData = projectData.get(SIMPLE_CREATION_PROJECT);
      assertThat(simpleCreationProjectData.getChangeLines(), IsIterableWithSize.iterableWithSize(0));
   }

   @Test
   void testDayOnMonthBorderCreation() {
      Map<String, ProjectData> projectData = mockAndCreateProjectData(DAY_ON_MONTH_BORDER_PROJECT, new DateTime(2022, 4, 1, 12, 22), PeassOverviewBuilder.LAST_DAY);

      assertThat(projectData.keySet(), IsIterableContaining.hasItem(DAY_ON_MONTH_BORDER_PROJECT));
      ProjectData simpleCreationProjectData = projectData.get(DAY_ON_MONTH_BORDER_PROJECT);
      assertThat(simpleCreationProjectData.getChangeLines(), IsIterableWithSize.iterableWithSize(1));
   }

   @Test
   void testLastWeekCreation() {
      Map<String, ProjectData> projectData = mockAndCreateProjectData(SIMPLE_CREATION_PROJECT, new DateTime(2022, 3, 25, 17, 22), PeassOverviewBuilder.LAST_WEEK);

      assertThat(projectData.keySet(), IsIterableContaining.hasItem(SIMPLE_CREATION_PROJECT));
      ProjectData simpleCreationProjectData = projectData.get(SIMPLE_CREATION_PROJECT);

      assertEquals("6ce9d6a3154c4ce8f617c357cf466fab222d27ef", simpleCreationProjectData.getChangeLines().get(0).getCommit());
   }

   private Map<String, ProjectData> mockAndCreateProjectData(String project, DateTime date, String timespan) {
      LinkedList<Project> projects = new LinkedList<>();
      projects.add(new Project(project));
      ProjectDataCreator creator = new ProjectDataCreator(projects, timespan);
      Run runMock = Mockito.mock(Run.class);
      Mockito.when(runMock.getRootDir()).thenReturn(virtualProjectFolder);

      DateTimeUtils.setCurrentMillisFixed(date.getMillis());

     return creator.generateAllProjectData(runMock, Mockito.mock(TaskListener.class));
   }


}
