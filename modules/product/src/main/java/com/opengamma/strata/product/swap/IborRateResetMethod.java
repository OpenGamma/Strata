/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A convention defining how to process a floating rate reset schedule.
 * <p>
 * When calculating interest, there may be multiple reset dates for a given accrual period.
 * This typically involves an average of a number of different rate fixings.
 */
public enum IborRateResetMethod {

  /**
   * The unweighted average method.
   * <p>
   * The result is a simple average of the applicable rates.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a(3C).
   */
  UNWEIGHTED,
  /**
   * The weighted average method.
   * <p>
   * The result is a weighted average of the applicable rates based on the
   * number of days each rate is applicable.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a(3D).
   */
  WEIGHTED;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborRateResetMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
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
