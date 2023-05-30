/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining accrued interest calculation type for inflation bond securities.
 * <p>
 * Yield of a bond security is a conventional number representing the internal rate of
 * return of standardized cash flows.
 * When calculating accrued interest, it is necessary to use a formula specific to each 
 * yield convention. Accordingly, the computation of price, convexity and duration from 
 * the yield should be based on this yield convention.
 * <p>
 * "Inflation Instruments: Swap Zero-coupon, Year-on-year and Bonds."
 */
public enum CapitalIndexedBondYieldConvention implements NamedEnum {

  /**
   * The US real yield convention. Used for TIPS (see Federal Register Vol. 69, N0. 170, p 53623).
   */
  US_IL_REAL("US-I/L-Real"),

  /**
   * The UK real yield convention. Used for inflation linked GILTS.
   */
  GB_IL_FLOAT("GB-I/L-Float"),

  /**
   * The UK real yield convention. Used for UK inflation linked corporate bond.
   */
  GB_IL_BOND("GB-I/L-Bond"),

  /**
   * The Japan simple real yield convention for inflation index bond.
   */
  JP_IL_SIMPLE("JP-I/L-Simple"),

  /**
   * The Japan compound real yield convention for inflation index bond.
   */
  JP_IL_COMPOUND("JP-I/L-Compound");

  // helper for name conversions
  private static final EnumNames<CapitalIndexedBondYieldConvention> NAMES =
      EnumNames.ofManualToString(CapitalIndexedBondYieldConvention.class);

  // name
  private final String name;

  // create
  private CapitalIndexedBondYieldConvention(String name) {
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
  public static CapitalIndexedBondYieldConvention of(String name) {
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
