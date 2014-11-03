/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * A convention defining how to compound interest.
 * <p>
 * When calculating interest, it may be necessary to apply compounding.
 * Compound interest occurs where the basic interest is collected over one period but paid over a longer period.
 * For example, interest may be collected every three months but only paid every year. 
 */
public enum CompoundingMethod {

  /**
   * No compounding applies.
   * <p>
   * This is typically used when the payment periods align with the accrual periods
   * thus no compounding is necessary. It may also be used when there are multiple
   * accrual periods, but they are not compounded.
   */
  NONE,
  /**
   * Flat compounding applies.
   * <p>
   * Defined as "Flat Compounding" in the ISDA 2006 definitions.
   */
  FLAT,
  /**
   * Spread inclusive compounding applies.
   * <p>
   * Defined as "Compounding" in the ISDA 2006 definitions.
   */
  STRAIGHT,
  /**
   * Spread exclusive compounding applies.
   * <p>
   * Defined as "Compounding treating Spread as simple interest" in the ISDA definitions.
   */
  SPREAD_EXCLUSIVE;

  //-------------------------------------------------------------------------
  /**
   * Obtains the compounding method from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the compounding method
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CompoundingMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Returns the formatted unique name of the compounding method.
   * 
   * @return the formatted string representing the compounding method
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
