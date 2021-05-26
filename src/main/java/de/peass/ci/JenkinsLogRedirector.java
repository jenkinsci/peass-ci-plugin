package de.peass.ci;

import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import hudson.model.TaskListener;

public class JenkinsLogRedirector implements AutoCloseable {
   
   private final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(JenkinsLogRedirector.class.getClassLoader(), false);

   private final PrintStream outOriginal;
   private final PrintStream errOriginal;
   private final OutputStreamAppender outputStreamAppender;

   public JenkinsLogRedirector(final TaskListener listener) {
      outOriginal = System.out;
      errOriginal = System.err;

      outputStreamAppender = OutputStreamAppender.newBuilder()
            .setName("jenkinslogger")
            .setTarget(listener.getLogger())
            .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36}:%L - %msg%n")
                  .build())
            .setConfiguration(loggerContext.getConfiguration()).build();
      outputStreamAppender.start();

      System.setOut(listener.getLogger());
      System.setErr(listener.getLogger());

      loggerContext.getConfiguration().addAppender(outputStreamAppender);
      loggerContext.getRootLogger().addAppender(loggerContext.getConfiguration().getAppender(outputStreamAppender.getName()));
      loggerContext.updateLoggers();
   }

   @Override
   public void close() {
      System.setOut(outOriginal);
      System.setErr(errOriginal);

      outputStreamAppender.stop();
      loggerContext.getConfiguration().getAppenders().remove(outputStreamAppender.getName());
      loggerContext.getRootLogger().removeAppender(outputStreamAppender);
   }

}
