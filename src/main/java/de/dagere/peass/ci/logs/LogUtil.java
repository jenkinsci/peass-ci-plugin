package de.dagere.peass.ci.logs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
   
   private static final Logger LOG = LogManager.getLogger(LogUtil.class);
   
   public static String mask(final String original, final Pattern pattern) {
      if (pattern != null) {
         Matcher matcher = pattern.matcher(original);
         LOG.debug("Found: " + matcher.find());
         if (matcher.find()) {
            LOG.debug("Replacing");
            String maskedLog = matcher.replaceAll("****");
            return maskedLog;
         }
      }
      return original;
   }
}
