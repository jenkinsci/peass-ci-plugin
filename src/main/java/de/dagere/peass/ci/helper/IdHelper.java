package de.dagere.peass.ci.helper;

public class IdHelper {
   private static int id = 0;
   
   public static int getId() {
      return id++;
   }
}
