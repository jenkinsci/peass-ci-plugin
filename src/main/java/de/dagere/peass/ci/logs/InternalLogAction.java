package de.dagere.peass.ci.logs;

import de.dagere.peass.ci.VisibleAction;

/**
 * Presents logs of Peass processes, i.e. regression test selection, measurement and root cause analysis
 * 
 * @author dagere
 *
 */
public class InternalLogAction extends VisibleAction {
   
   private final String displayName;
   private final String title;
   private final String text;

   public InternalLogAction(final String displayName, final String title, final String text) {
      this.displayName = displayName;
      this.title = title;
      this.text = text;
   }

   @Override
   public String getUrlName() {
      return displayName;
   }
   
   public String getTitle() {
      return title;
   }

   public String getText() {
      return text;
   }

   @Override
   public String getIconFileName() {
      return null;
   }

   @Override
   public String getDisplayName() {
      return null;
   }
}
