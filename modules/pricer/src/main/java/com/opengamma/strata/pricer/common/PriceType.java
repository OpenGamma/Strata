/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.common;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Enumerates the types of price that can be returned. 
 * <p>
 * The price is usually clean or dirty.
 * Financial instruments are often quoted in terms of clean price rather than dirty price.
 * <p>
 * The dirty price is the full price, which is typically the mark-to-market value of an instrument.
 * <p>
 * The clean price is computed from the dirty price by subtracting/adding accrued interest.
 * Subtraction/addition is determined by the direction of accrued interest payment.  
 */
public enum PriceType implements NamedEnum {

  /**
   * Clean price.
   * <p>
   * The accrued interest is removed from the full price.
   */
  CLEAN,
  /**
   * Dirty price.
   * <p>
   * The dirty price is the full price of an instrument.
   */
  DIRTY;

  // helper for name conversions
  private static final EnumNames<PriceType> NAMES = EnumNames.of(PriceType.class);

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
  public static PriceType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Check if the price type is 'Clean'.
   * 
   * @return true if clean, false if dirty
   */
  public boolean isCleanPrice() {
    return this == CLEAN;
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
