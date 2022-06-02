package de.dagere.peass.ci.peassAnalysis;

import java.io.Serializable;

public class ChangeLine implements Serializable {
   private static final long serialVersionUID = 1L;

   private String version;
   private String changedEntity;
   private String testcase;
   private double changePercent;

   public ChangeLine() {
   }

   public ChangeLine(String version, String changedEntity, String testcase, double changePercent) {
      this.version = version;
      this.changedEntity = changedEntity;
      this.testcase = testcase;
      this.changePercent = changePercent;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
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