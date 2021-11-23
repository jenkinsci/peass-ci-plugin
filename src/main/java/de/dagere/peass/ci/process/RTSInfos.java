package de.dagere.peass.ci.process;

public class RTSInfos {
   private final boolean staticChanges;
   private final boolean staticallySelectedTests;

   public RTSInfos(final boolean staticChanges, final boolean staticallySelectedTests) {
      this.staticChanges = staticChanges;
      this.staticallySelectedTests = staticallySelectedTests;
   }

   public boolean isStaticChanges() {
      return staticChanges;
   }

   public boolean isStaticallySelectedTests() {
      return staticallySelectedTests;
   }

}
