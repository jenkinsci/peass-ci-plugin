package de.dagere.peass.ci.process;

import java.util.LinkedList;
import java.util.List;

public class IncludeExcludeParser {
   public static List<String> getStringList(final String raw) {
      StringBuilder errorMessageBuilder = new StringBuilder();
      List<String> includeList = new LinkedList<>();
      if (raw != null && raw.trim().length() > 0) {
         final String nonSpaceIncludes = raw.replaceAll(" ", "");
         for (String include : nonSpaceIncludes.split(";")) {
            includeList.add(include);
            if (!include.contains("#")) {
               errorMessageBuilder.append("Include ")
                     .append(include)
                     .append(" does not contain #; this will not match any method. ");
            }
         }
      }
      if (errorMessageBuilder.length() > 0) {
         throw new RuntimeException("Please always add includes in the form package.Class#method, and if you want to include all methods package.Class#*. "
               + " The following includes contained problems: "
               + errorMessageBuilder);
      }
      return includeList;
   }
}
