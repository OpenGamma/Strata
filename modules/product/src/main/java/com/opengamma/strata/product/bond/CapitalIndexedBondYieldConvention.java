/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.util.Locale;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;

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
public enum CapitalIndexedBondYieldConvention {

  /**
   * The US real yield convention. Used for TIPS (see Federal Register Vol. 69, N0. 170, p 53623).
   */
  US_IL_REAL("US-I/L-Real"),

  /**
   * The UK real yield convention. Used for inflation linked GILTS.
   */
  INDEX_LINKED_FLOAT("Index-Linked-Float"),

  /**
   * The UK real yield convention. Used for UK inflation linked corporate bond.
   */
  UK_IL_BOND("UK-I/L-Bond");

  // name
  private final String name;

  // create
  private CapitalIndexedBondYieldConvention(String name) {
    this.name = name;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CapitalIndexedBondYieldConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(uniqueName.replace('-', '_').replace("/", "").toUpperCase(Locale.ENGLISH));
  }

  //-------------------------------------------------------------------------
  /**
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return name;
  }

}
