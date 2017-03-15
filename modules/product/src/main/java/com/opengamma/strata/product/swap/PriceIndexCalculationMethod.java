/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Reference price index calculation method.
 * <p>
 * This defines how the reference index calculation occurs.
 * <p>
 * References: "Bond Pricing", OpenGamma Documentation 5, Version 2.0, May 2013, 
 * "Inflation Instruments: Swap Zero-coupon, Year-on-year and Bonds."
 */
public enum PriceIndexCalculationMethod {

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

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PriceIndexCalculationMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
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
