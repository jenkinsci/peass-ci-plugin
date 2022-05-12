package de.dagere.peass.ci.peassAnalysis;

import java.io.Serializable;

public class ChangeLine implements Serializable {
   private static final long serialVersionUID = 1L;

   private String version;
   private String change;
   private String testcase;
   private boolean measured;

   public ChangeLine() {
   }

   public ChangeLine(String version, String change, String testcase, boolean measured) {
      this.version = version;
      this.change = change;
      this.testcase = testcase;
      this.measured = measured;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getChange() {
      return change;
   }

   public void setChange(String change) {
      this.change = change;
   }

   public String getTestcase() {
      return testcase;
   }

   public void setTestcase(String testcase) {
      this.testcase = testcase;
   }

   public boolean isMeasured() {
      return measured;
   }

   public void setMeasured(boolean measured) {
      this.measured = measured;
   }

}