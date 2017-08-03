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
 * The method of accruing interest based on an Overnight index.
 * <p>
 * Two methods of accrual are supported - compounded and averaged.
 * Averaging is primarily related to the 'USD-FED-FUND' index.
 */
public enum OvernightAccrualMethod implements NamedEnum {

  /**
   * The compounded method.
   * <p>
   * Interest is accrued by simple compounding of each rate published during the accrual period.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a(3C).
   */
  COMPOUNDED,
  /**
   * The averaged method.
   * <p>
   * Interest is accrued by taking the average of all the rates published on the
   * index during the accrual period.
   */
  AVERAGED;

  // helper for name conversions
  private static final EnumNames<OvernightAccrualMethod> NAMES = EnumNames.of(OvernightAccrualMethod.class);

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
  public static OvernightAccrualMethod of(String name) {
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
