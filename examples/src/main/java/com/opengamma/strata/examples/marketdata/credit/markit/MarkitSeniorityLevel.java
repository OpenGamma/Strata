/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

import com.opengamma.strata.finance.credit.SeniorityLevel;

/**
 * Specifies the repayment precedence of a debt instrument.
 * Aka Tier
 */
public enum MarkitSeniorityLevel {

  /**
   * Senior domestic (RED Tier Code: SECDOM).
   */

  SECDOM,

  /**
   * Senior foreign (RED Tier Code: SNRFOR).
   */

  SNRFOR,

  /**
   * Subordinate, Lower Tier 2 (RED Tier Code: SUBLT2).
   */

  SUBLT2,

  /**
   * Subordinate Tier 1 (RED Tier Code: PREFT1).
   */

  PREFT1,

  /**
   * Subordinate, Upper Tier 2 (RED Tier Code: JRSUBUT2).
   */

  JRSUBUT2;

  public SeniorityLevel translate() {
    switch (this) {
      case SECDOM:
        return SeniorityLevel.SeniorSecuredDomestic;
      case SNRFOR:
        return SeniorityLevel.SeniorUnsecuredForeign;
      case SUBLT2:
        return SeniorityLevel.SubordinateLowerTier2;
      case PREFT1:
        return SeniorityLevel.SubordinateTier1;
      case JRSUBUT2:
        return SeniorityLevel.SubordinateUpperTier2;
      default:
        throw new IllegalStateException("Unmapped seniority level. Do not have mapping for " + this);
    }
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
