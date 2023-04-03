package de.dagere.peass.ci.rts;

import java.util.LinkedList;
import java.util.List;

import de.dagere.nodeDiffDetector.data.MethodCall;

public class LineUtil {

   private static final int MAX_CHAR_COUNT = 90;

   public static List<String> createPrintable(String originalTestcase) {
      List<String> printableTestcase = new LinkedList<>();
      if (originalTestcase.length() > MAX_CHAR_COUNT) {
         int moduleIndex = originalTestcase.lastIndexOf(MethodCall.MODULE_SEPARATOR);
         if (moduleIndex != -1) {
            printableTestcase.add(originalTestcase.substring(0, moduleIndex + 1));
         }
         int methodSeperatorIndex = originalTestcase.indexOf(MethodCall.METHOD_SEPARATOR);
         if (methodSeperatorIndex != -1 && originalTestcase.length() - moduleIndex > MAX_CHAR_COUNT) {
            printableTestcase.add(originalTestcase.substring(moduleIndex + 1, methodSeperatorIndex + 1));
            int parameterIndex = originalTestcase.indexOf('(');
            if (parameterIndex != -1) {
               String parameterString = originalTestcase.substring(parameterIndex);
               if (parameterString.length() > MAX_CHAR_COUNT) {
                  printableTestcase.add(parameterString.replaceAll(",", ", "));
               } else {
                  printableTestcase.add(parameterString);
               }
            } else {
               printableTestcase.add(originalTestcase.substring(methodSeperatorIndex + 1));
            }
         } else {
            printableTestcase.add(originalTestcase.substring(moduleIndex + 1));
         }
      } else {
         printableTestcase.add(originalTestcase);
      }
      return printableTestcase;
   }
}
