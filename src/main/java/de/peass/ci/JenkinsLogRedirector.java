package de.peass.ci;

import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import hudson.model.TaskListener;

public class JenkinsLogRedirector implements AutoCloseable {
   
   private final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);
   
   private final PrintStream outOriginal;
   private final PrintStream errOriginal;
   private final OutputStreamAppender fa;
   
   public JenkinsLogRedirector(final TaskListener listener) {
      outOriginal = System.out;
      errOriginal = System.err;
      
      fa = OutputStreamAppender.newBuilder()
            .setName("jenkinslogger")
            .setTarget(listener.getLogger())
            .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n")
                  .build())
            .setConfiguration(loggerContext.getConfiguration()).build();
      fa.start();
      
      System.setOut(listener.getLogger());
      System.setErr(listener.getLogger());
      
      loggerContext.getConfiguration().addAppender(fa);
      loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(fa.getName()));
      loggerContext.updateLoggers();
   }
   
   @Override
   public void close() {
      System.setOut(outOriginal);
      System.setErr(errOriginal);

      fa.stop();
      loggerContext.getConfiguration().getAppenders().remove(fa.getName());
      loggerContext.getRootLogger().removeAppender(fa);
   }

}
