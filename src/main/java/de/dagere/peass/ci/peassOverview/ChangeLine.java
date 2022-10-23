package de.dagere.peass.ci.peassOverview;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.ci.rts.LineUtil;

public class ChangeLine implements Serializable {
   private static final long serialVersionUID = 1L;

   private String commit;
   private List<String> changedEntity;
   private String testcase;
   private double changePercent;
   private DynamicallyUnselected dynamicallyUnselected;

   public ChangeLine() {
   }

   public ChangeLine(String commit, List<String> changedEntity, String testcase, double changePercent, DynamicallyUnselected dynamicallyUnselected) {
      this.commit = commit;
      this.changedEntity = changedEntity;
      this.testcase = testcase;
      this.changePercent = changePercent;
      this.dynamicallyUnselected = dynamicallyUnselected;
   }

   public String getCommit() {
      return commit;
   }

   public void setCommit(String commit) {
      this.commit = commit;
   }

   public List<String> getChange() {
      return changedEntity;
   }

   public void setChange(List<String> change) {
      this.changedEntity = change;
   }

   public List<String> getChangeVisible() {
      List<String> result = new LinkedList<>();
      for (String line : changedEntity) {
         result.addAll(LineUtil.createPrintable(line));
      }
      return result;
   }

   public String getTestcase() {
      return testcase;
   }

   public void setTestcase(String testcase) {
      this.testcase = testcase;
   }

   public DynamicallyUnselected getDynamicallyUnselected() {
      return dynamicallyUnselected;
   }

   public void setDynamicallyUnselected(DynamicallyUnselected dynamicallyUnselected) {
      this.dynamicallyUnselected = dynamicallyUnselected;
   }

   public List<String> getTestcaseVisible() {
      return LineUtil.createPrintable(testcase);
   }

   public boolean isNaN(double value) {
      return Double.isNaN(value);
   }

   public double getChangePercent() {
      return changePercent;
   }

   public void setChangePercent(double changePercent) {
      this.changePercent = changePercent;
   }
}