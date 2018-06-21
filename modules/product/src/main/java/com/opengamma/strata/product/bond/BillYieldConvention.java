/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining how yield is computed for a bill.
 */
public enum BillYieldConvention implements NamedEnum {
  
  /**
   * Discount.
   */
  DISCOUNT("Discount"),
  
  /**
   * France CD: interest at maturity.
   */
  FRANCE_CD("France-CD"),
  
  /**
   * Interest at maturity.
   */
  INTEREST_AT_MATURITY("Interest-at-maturity"),
  
  /**
   * Japanese T-Bills.
   */
  JAPAN_BILL("Japan-Bills");

  // helper for name conversions
  private static final EnumNames<BillYieldConvention> NAMES =
      EnumNames.ofManualToString(BillYieldConvention.class);

  // name
  private final String name;

  // create
  private BillYieldConvention(String name) {
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
  public static BillYieldConvention of(String name) {
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
