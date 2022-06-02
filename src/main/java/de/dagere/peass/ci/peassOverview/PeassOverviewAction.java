package de.dagere.peass.ci.peassOverview;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import org.jvnet.localizer.LocaleProvider;

import de.dagere.peass.ci.VisibleAction;

public class PeassOverviewAction extends VisibleAction {

   private final Map<String, ProjectData> projects;
   
   public PeassOverviewAction(int id, Map<String, ProjectData> projects) {
      super(id);
      this.projects = projects;
   }
   
   public Map<String, ProjectData> getProjects() {
      return projects;
   }

   @Override
   public String getIconFileName() {
      return "/plugin/peass-ci/images/overview.png";
   }

   @Override
   public String getDisplayName() {
      return "Peass Measurement Overview";
   }

   @Override
   public String getUrlName() {
      return "overview";
   }
   
   public String round(final double value) {
      double roundedValue = Math.round(value * 10000) / 10000d;
      Locale locale = LocaleProvider.getLocale();
      return NumberFormat.getInstance(locale).format(roundedValue);
   }

}
