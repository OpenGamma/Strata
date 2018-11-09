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
 * The method of accruing interest on a notional amount using a fixed rate.
 */
public enum FixedAccrualMethod implements NamedEnum {

  /**
   * The default method.
   * <p>
   * The notional accrues no interest.
   * <p>
   * This is the most common type for fixed legs.
   */
  DEFAULT,
  /**
   * Defines overnight compounding using an an annual rate.
   * <p>
   * The notional accrues interest on an overnight basis using an annual fixed rate.
   * <p>
   * This is the most common type for Brazilian style fixed legs.
   */
  OVERNIGHT_COMPOUNDED_ANNUAL_RATE;

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
