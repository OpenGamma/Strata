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
 * The type of a swap leg.
 * <p>
 * This provides a high-level categorization of a swap leg.
 * This is useful when it is necessary to find a specific leg.
 */
public enum SwapLegType implements NamedEnum {

  /**
   * A fixed rate swap leg.
   * All periods in this leg must have a fixed rate.
   */
  FIXED,
  /**
   * A floating rate swap leg based on an Ibor index.
   * <p>
   * This kind of leg may include some fixed periods, such as in a stub or
   * where the first rate is specified in the contract.
   */
  IBOR,
  /**
   * A floating rate swap leg based on an Overnight index.
   * <p>
   * This kind of leg may include some fixed periods, such as in a stub or
   * where the first rate is specified in the contract.
   */
  OVERNIGHT,
  /**
   * A floating rate swap leg based on an price index.
   * <p>
   * This kind of leg may include some reference dates 
   * where the index rate is specified.
   */
  INFLATION,
  /**
   * A swap leg that is not based on a Fixed, Ibor, Overnight or Inflation rate.
   */
  OTHER;

  // helper for name conversions
  private static final EnumNames<SwapLegType> NAMES = EnumNames.of(SwapLegType.class);

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
  public static SwapLegType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Fixed'.
   * 
   * @return true if fixed, false otherwise
   */
  public boolean isFixed() {
    return this == FIXED;
  }

  /**
   * Checks if the type is floating, defined as 'Ibor', 'Overnight' or 'Inflation'.
   * 
   * @return true if floating, false otherwise
   */
  public boolean isFloat() {
    return this == IBOR || this == OVERNIGHT || this == INFLATION;
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
