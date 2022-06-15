package de.dagere.peass.ci.process;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import kieker.monitoring.core.signaturePattern.InvalidPatternException;
import kieker.monitoring.core.signaturePattern.PatternParser;

public class IncludeExcludeParser {

   public static LinkedHashSet<String> getStringSet(final String raw) {
      LinkedHashSet<String> includeList = new LinkedHashSet<>();
      if (raw != null && raw.trim().length() > 0) {
         final String nonSpaceIncludes = raw.trim();
         for (String include : nonSpaceIncludes.split(";")) {
            if (include.length() > 0) {
               try {
                  PatternParser.parseToPattern(include);
               } catch (InvalidPatternException e) {
                  throw new RuntimeException("Can not parse pattern " + include, e);
               }
               includeList.add(include);
            }
         }
      }
      return includeList;
   }

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
   
   public static List<String> getStringListSimple(final String raw) {
      List<String> includeList = new LinkedList<>();
      if (raw != null && raw.trim().length() > 0) {
         final String nonSpaceIncludes = raw.replaceAll(" ", "");
         for (String include : nonSpaceIncludes.split(";")) {
            includeList.add(include);
         }
      }
      return includeList;
   }
}
