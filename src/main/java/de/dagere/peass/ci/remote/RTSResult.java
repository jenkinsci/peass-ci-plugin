package de.dagere.peass.ci.remote;

import java.io.Serializable;
import java.util.Set;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class RTSResult implements Serializable {
   private static final long serialVersionUID = 700041797958688300L;

   private final Set<TestCase> tests;
   private final String versionOld;

   public RTSResult(final Set<TestCase> tests, final String versionOld) {
      this.tests = tests;
      this.versionOld = versionOld;
   }

   public Set<TestCase> getTests() {
      return tests;
   }

   public String getVersionOld() {
      return versionOld;
   }

}
