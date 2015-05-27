package com.opengamma.strata.finance.credit.general.reference;

/**
 * http://www.fpml.org/coding-scheme/credit-seniority
 *
 * Specifies the repayment precedence of a debt instrument.
 */
public enum SeniorityLevel {

  SeniorSec("SECDOM", true), // Senior domestic (RED Tier Code: SECDOM).
  SeniorUnSec("SNRFOR", true), // Senior foreign (RED Tier Code: SNRFOR).
  SubLowerTier2("SUBLT2", false), // Subordinate, Lower Tier 2 (RED Tier Code: SUBLT2).
  SubTier1("PREFT1", false), // Subordinate Tier 1 (RED Tier Code: PREFT1).
  SubUpperTier2("JRSUBUT2", false); // Subordinate, Upper Tier 2 (RED Tier Code: JRSUBUT2).

  private String redTierCode;
  private boolean isSenior;

  SeniorityLevel(String redTierCode, boolean isSenior) {
    this.redTierCode = redTierCode;
    this.isSenior = isSenior;
  }

  String getRedTierCode() {
    return redTierCode;
  }

  boolean isSenior() {
    return isSenior;
  }

}
