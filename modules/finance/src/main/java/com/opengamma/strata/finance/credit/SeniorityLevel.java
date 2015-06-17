/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

/**
 * http://www.fpml.org/coding-scheme/credit-seniority
 * <p>
 * Specifies the repayment precedence of a debt instrument.
 * Aka Tier
 */
public enum SeniorityLevel {

  /**
   * Senior domestic.
   */
  SeniorSecuredDomestic,

  /**
   * Senior foreign.
   */
  SeniorUnsecuredForeign,

  /**
   * Subordinate, Lower Tier 2.
   */
  SubordinateLowerTier2,

  /**
   * Subordinate Tier 1.
   */
  SubordinateTier1,

  /**
   * Subordinate, Upper Tier 2.
   */
  SubordinateUpperTier2;


}
