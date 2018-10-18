/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The method of accruing interest based on the fixed rate of a fixed swap leg.
 * <p>
 * Two methods of accrual are supported - simple and compounded.
 */
public enum FixedAccrualMethod implements NamedEnum {

  /**
   * The simple method.
   * <p>
   * Simple interest is accrued. This is determined by multiplying the fixed rate by the notional and the accrual factor.
   * <></>
   * This is the most common type for vanilla swaps.
   */
  SIMPLE,

  /**
   * The BRL style compounded method.
   * <p>
   * OpenGamma, "Brazilian Swaps" (2013).
   * <p>
   * This is the most common type for Brazilian style swaps.
   */
   BRL_COMPOUNDED;

  // helper for name conversions
  private static final EnumNames<FixedAccrualMethod> NAMES = EnumNames.of(FixedAccrualMethod.class);

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
  public static FixedAccrualMethod of(String name) {
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
