/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.markit;

import com.opengamma.strata.finance.credit.SeniorityLevel;

/**
 * Specifies the repayment precedence of a debt instrument.
 * Aka Tier
 */
public enum MarkitSeniorityLevel {

  /**
   * Senior domestic (RED Tier Code: SECDOM).
   */

  SECDOM(true),

  /**
   * Senior foreign (RED Tier Code: SNRFOR).
   */

  SNRFOR(true),

  /**
   * Subordinate, Lower Tier 2 (RED Tier Code: SUBLT2).
   */

  SUBLT2(false),

  /**
   * Subordinate Tier 1 (RED Tier Code: PREFT1).
   */

  PREFT1(false),

  /**
   * Subordinate, Upper Tier 2 (RED Tier Code: JRSUBUT2).
   */

  JRSUBUT2(false);

  private final boolean isSenior;

  MarkitSeniorityLevel(boolean isSenior) {
    this.isSenior = isSenior;
  }

  /**
   * @return whether this is a senior issue or not, which will drive the recovery rate applied in pricing
   */
  public boolean isSenior() {
    return isSenior;
  }

  public static MarkitSeniorityLevel from(SeniorityLevel seniorityLevel) {
    switch (seniorityLevel) {
      case SeniorSecuredDomestic:
        return SECDOM;
      case SeniorUnsecuredForeign:
        return SNRFOR;
      case SubordinateLowerTier2:
        return SUBLT2;
      case SubordinateTier1:
        return PREFT1;
      case SubordinateUpperTier2:
        return JRSUBUT2;
      default:
        throw new UnsupportedOperationException("Unknown seniority level. Do not have mapping for " + seniorityLevel);
    }
  }

}
