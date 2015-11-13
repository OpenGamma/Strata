/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * Specifies the repayment precedence of a debt instrument.
 * <p>
 * This is also known as the tier.
 */
public enum SeniorityLevel {

  /**
   * Senior domestic.
   */
  SENIOR_SECURED_DOMESTIC,

  /**
   * Senior foreign.
   */
  SENIOR_UNSECURED_FOREIGN,

  /**
   * Subordinate, Lower Tier 2.
   */
  SUBORDINATE_LOWER_TIER_2,

  /**
   * Subordinate, Tier 1.
   */
  SUBORDINATE_TIER_1,

  /**
   * Subordinate, Upper Tier 2.
   */
  SUBORDINATE_UPPER_TIER_2;

}
