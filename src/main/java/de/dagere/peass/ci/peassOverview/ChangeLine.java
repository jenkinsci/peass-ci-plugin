package de.dagere.peass.ci.peassOverview;

import java.io.Serializable;

public class ChangeLine implements Serializable {
   private static final long serialVersionUID = 1L;

   private String commit;
   private String changedEntity;
   private String testcase;
   private double changePercent;

   public ChangeLine() {
   }

   public ChangeLine(String commit, String changedEntity, String testcase, double changePercent) {
      this.commit = commit;
      this.changedEntity = changedEntity;
      this.testcase = testcase;
      this.changePercent = changePercent;
   }

   public String getCommit() {
      return commit;
   }

   public void setCommit(String commit) {
      this.commit = commit;
   }

   public String getChange() {
      return changedEntity;
   }

   public void setChange(String change) {
      this.changedEntity = change;
   }

   public String getTestcase() {
      return testcase;
   }

   public void setTestcase(String testcase) {
      this.testcase = testcase;
   }
   
   public double getChangePercent() {
      return changePercent;
   }
   
   public void setChangePercent(double changePercent) {
      this.changePercent = changePercent;
   }
}