/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining accrued interest calculation type for a bond security.
 * <p>
 * Yield of a bond security is a conventional number representing the internal rate of
 * return of standardized cash flows.
 * When calculating accrued interest, it is necessary to use a formula specific to each
 * yield convention. Accordingly, the computation of price, convexity and duration from
 * the yield should be based on this yield convention.
 * <p>
 * References: "Bond Pricing", OpenGamma Documentation 5, Version 2.0, May 2013
 */
public enum FixedCouponBondYieldConvention implements NamedEnum {

  /**
   * UK BUMP/DMO method.
   */
  GB_BUMP_DMO("GB-Bump-DMO"),

  /**
   * US street.
   */
  US_STREET("US-Street"),

  /**
   * German bonds.
   */
  DE_BONDS("DE-Bonds"),

  /**
   * Japan simple yield.
   */
  JP_SIMPLE("JP-Simple");

  // helper for name conversions
  private static final EnumNames<FixedCouponBondYieldConvention> NAMES =
      EnumNames.ofManualToString(FixedCouponBondYieldConvention.class);

  // name
  private final String name;

  // create
  private FixedCouponBondYieldConvention(String name) {
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
  public static FixedCouponBondYieldConvention of(String name) {
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
