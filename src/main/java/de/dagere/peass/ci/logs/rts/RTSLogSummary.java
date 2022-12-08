package de.dagere.peass.ci.logs.rts;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

/**
 * This summarizes logs for showing information in the regular step action.
 *
 * Currently, it only contains a boolean, but it is expected to contain more information for the end user soon.
 *
 * @author reichelt
 *
 */
public class RTSLogSummary {
   
   private static final Logger LOG = LogManager.getLogger(RTSLogSummary.class);

   public static RTSLogSummary createLogSummary(Map<TestMethodCall, RTSLogData> rtsVmRuns, Map<TestMethodCall, RTSLogData> rtsVmRunsPredecessor) {
      boolean commitContainsFailure = rtsVmRuns.values().stream().anyMatch(log -> !log.isSuccess() && !log.isIgnored());
      boolean predecessorContainsFailure = rtsVmRunsPredecessor.values().stream().anyMatch(log -> !log.isSuccess() && !log.isIgnored());

      boolean commitContainsSuccess = rtsVmRuns.values().stream().anyMatch(log -> log.isSuccess());
      boolean predecessorContainsSuccess = rtsVmRunsPredecessor.values().stream().anyMatch(log -> log.isSuccess());

      boolean commitContainsParametrizedwhithoutIndex = rtsVmRuns.values().stream().anyMatch(log -> log.isParameterizedWithoutIndex());
      boolean predecessorContainsParametrizedwhithoutIndex = rtsVmRunsPredecessor.values().stream().anyMatch(log -> log.isParameterizedWithoutIndex());

      LOG.debug("Errors in logs: current: {} predecessor: {}", commitContainsFailure, predecessorContainsFailure);
      RTSLogSummary logSummary = new RTSLogSummary(commitContainsFailure, predecessorContainsFailure, commitContainsSuccess, predecessorContainsSuccess,
            commitContainsParametrizedwhithoutIndex, predecessorContainsParametrizedwhithoutIndex);
      return logSummary;
   }

   private final boolean errorInCurrentCommitOccured;
   private final boolean errorInPredecessorCommitOccured;
   private final boolean commitContainsSuccess;
   private final boolean predecessorContainsSuccess;
   private final boolean commitContainsParametrizedwhithoutIndex;
   private final boolean predecessorContainsParametrizedwhithoutIndex;

   public RTSLogSummary(final boolean errorInCurrentCommitOccured, final boolean errorInPredecessorCommitOccured, boolean commitContainsSuccess,
         boolean predecessorContainsSuccess, boolean commitContainsParametrizedwhithoutIndex, boolean predecessorContainsParametrizedwhithoutIndex) {
      this.errorInCurrentCommitOccured = errorInCurrentCommitOccured;
      this.errorInPredecessorCommitOccured = errorInPredecessorCommitOccured;
      this.commitContainsSuccess = commitContainsSuccess;
      this.predecessorContainsSuccess = predecessorContainsSuccess;
      this.commitContainsParametrizedwhithoutIndex = commitContainsParametrizedwhithoutIndex;
      this.predecessorContainsParametrizedwhithoutIndex = predecessorContainsParametrizedwhithoutIndex;
   }

   public boolean isErrorInCurrentCommitOccured() {
      return errorInCurrentCommitOccured;
   }

   public boolean isErrorInPredecessorCommitOccured() {
      return errorInPredecessorCommitOccured;
   }

   public boolean isCommitContainsSuccess() {
      return commitContainsSuccess;
   }

   public boolean isPredecessorContainsSuccess() {
      return predecessorContainsSuccess;
   }

   public boolean isCommitContainsParametrizedwhithoutIndex() {
      return commitContainsParametrizedwhithoutIndex;
   }

   public boolean isPredecessorContainsParametrizedwhithoutIndex() {
      return predecessorContainsParametrizedwhithoutIndex;
   }

}
