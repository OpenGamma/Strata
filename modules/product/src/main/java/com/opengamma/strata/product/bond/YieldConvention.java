/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A convention defining accrued interest calculation type for a bond security. 
 * <p>
 * Yield of a bond security is a conventional number representing the internal rate of
 * return of standardized cash flows.
 * When calculating accrued interest from the yield, it is necessary to use a formula
 * specific to each yield convention. Accordingly, the computation of price, convexity
 * and duration from the yield should be based on this yield convention. 
 * <p>
 * Reference: "Bond Pricing", OpenGamma Documentation 5, Version 2.0, May 2013.
 */
public enum YieldConvention {

  /**
   * UK BUMP/DMO method. 
   */
  UK_BUMP_DMO,

  /**
   * US Street convention.
   */
  US_STREET,

  /**
   * German bonds. 
   */
  GERMAN_BONDS,

  /**
   * Japan Simple yield. 
   */
  JAPAN_SIMPLE;

  //-------------------------------------------------------------------------
  /**
   * Obtains the convention from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the yield convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static YieldConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(uniqueName);
  }

}
