/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import java.util.Locale;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A convention defining how to discount Forward Rate Agreements (FRAs).
 * <p>
 * When calculating the price of a FRA, there are different approaches to pricing in different markets.
 * This method captures the approach to discounting.
 * <p>
 * Defined by the 2006 ISDA definitions article 8.4.
 */
public enum FraDiscountingMethod {

  /**
   * No discounting applies.
   */
  NONE("None"),
  /**
   * FRA discounting as defined by ISDA.
   * <p>
   * Defined by the 2006 ISDA definitions article 8.4b.
   */
  ISDA("ISDA"),
  /**
   * FRA discounting as defined by the Australian Financial Markets Association (AFMA).
   * <p>
   * Defined by the 2006 ISDA definitions article 8.4e.
   */
  AFMA("AFMA");

  // name
  private final String name;

  // create
  private FraDiscountingMethod(String name) {
    this.name = name;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the compounding method from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the compounding method
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FraDiscountingMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(uniqueName.toUpperCase(Locale.ENGLISH));
  }

  /**
   * Returns the formatted unique name of the compounding method.
   * 
   * @return the formatted string representing the compounding method
   */
  @ToString
  @Override
  public String toString() {
    return name;
  }

}
