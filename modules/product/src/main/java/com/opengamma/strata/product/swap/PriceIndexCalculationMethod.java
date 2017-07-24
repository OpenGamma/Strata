/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Reference price index calculation method.
 * <p>
 * This defines how the reference index calculation occurs.
 * <p>
 * References: "Bond Pricing", OpenGamma Documentation 5, Version 2.0, May 2013, 
 * "Inflation Instruments: Swap Zero-coupon, Year-on-year and Bonds."
 */
public enum PriceIndexCalculationMethod implements NamedEnum {

  /**
   * The reference index is the price index of a month.
   * The reference month is linked to the payment date.
   */
  MONTHLY,
  /**
   * The reference index is linearly interpolated between two months.
   * The interpolation is done with the number of days of the payment month.
   * The number of days is counted from the beginning of the month.
   */
  INTERPOLATED,
  /**
   * The reference index is linearly interpolated between two months.
   * The interpolation is done with the number of days of the payment month.
   * The number of days is counted from the 10th day of the month.
   */
  INTERPOLATED_JAPAN;

  // helper for name conversions
  private static final EnumNames<PriceIndexCalculationMethod> NAMES = EnumNames.of(PriceIndexCalculationMethod.class);

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
  public static PriceIndexCalculationMethod of(String name) {
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
