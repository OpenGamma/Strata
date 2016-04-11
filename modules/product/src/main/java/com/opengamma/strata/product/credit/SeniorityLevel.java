/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

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

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SeniorityLevel of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    String str = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName);
    if (str.endsWith("1") || str.endsWith("2")) {
      return valueOf(str.substring(0, str.length() - 1) + "_" + str.substring(str.length() - 1));
    }
    return valueOf(str);
  }

  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
