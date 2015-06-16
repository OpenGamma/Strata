/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.common;

/**
 * http://www.fpml.org/coding-scheme/credit-seniority
 *
 * Specifies the repayment precedence of a debt instrument.
 * Aka Tier
 */
public enum SeniorityLevel {

  /**
   * Senior domestic (RED Tier Code: SECDOM).
   */
  SeniorSec("SECDOM", true),

  /**
   * Senior foreign (RED Tier Code: SNRFOR).
   */
  SeniorUnSec("SNRFOR", true),

  /**
   * Subordinate, Lower Tier 2 (RED Tier Code: SUBLT2).
   */
  SubLowerTier2("SUBLT2", false),

  /**
   * Subordinate Tier 1 (RED Tier Code: PREFT1).
   */
  SubTier1("PREFT1", false),

  /**
   * Subordinate, Upper Tier 2 (RED Tier Code: JRSUBUT2).
   */
  SubUpperTier2("JRSUBUT2", false);

  private String redTierCode;
  private boolean isSenior;

  SeniorityLevel(String redTierCode, boolean isSenior) {
    this.redTierCode = redTierCode;
    this.isSenior = isSenior;
  }

  /**
   * @return Common Markit/RED abbreviation
   */
  public String getRedTierCode() {
    return redTierCode;
  }

  /**
   * @return whether this is a senior issue or not, which will drive the recovery rate applied in pricing
   */
  public boolean isSenior() {
    return isSenior;
  }

}
