/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining how to process a floating rate reset schedule.
 * <p>
 * When calculating interest, there may be multiple reset dates for a given accrual period.
 * This typically involves an average of a number of different rate fixings.
 */
public enum IborRateResetMethod implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<IborRateResetMethod> NAMES = EnumNames.of(IborRateResetMethod.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborRateResetMethod of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
