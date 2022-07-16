package de.dagere.peass.ci.rts;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class LineUtil {

   public static List<String> createPrintable(String originalTestcase) {
      List<String> printableTestcase = new LinkedList<>();
      if (originalTestcase.length() > 80) {
         int moduleIndex = originalTestcase.lastIndexOf(ChangedEntity.MODULE_SEPARATOR);
         if (moduleIndex != -1) {
            printableTestcase.add(originalTestcase.substring(0, moduleIndex + 1));
         }
         int methodSeperatorIndex = originalTestcase.indexOf(ChangedEntity.METHOD_SEPARATOR);
         if (methodSeperatorIndex != -1) {
            printableTestcase.add(originalTestcase.substring(moduleIndex + 1, methodSeperatorIndex + 1));
            int parameterIndex = originalTestcase.indexOf('(');
            if (parameterIndex != -1) {
               String parameterString = originalTestcase.substring(parameterIndex);
               if (parameterString.length() > 80) {
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
