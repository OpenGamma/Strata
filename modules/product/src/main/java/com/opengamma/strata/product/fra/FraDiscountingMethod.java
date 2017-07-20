/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining how to discount Forward Rate Agreements (FRAs).
 * <p>
 * When calculating the price of a FRA, there are different approaches to pricing in different markets.
 * This method captures the approach to discounting.
 * <p>
 * Defined by the 2006 ISDA definitions article 8.4.
 */
public enum FraDiscountingMethod implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<FraDiscountingMethod> NAMES = EnumNames.ofManualToString(FraDiscountingMethod.class);

  // name
  private final String name;

  // create
  private FraDiscountingMethod(String name) {
    this.name = name;
  }

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
  public static FraDiscountingMethod of(String name) {
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
    return name;
  }

}
