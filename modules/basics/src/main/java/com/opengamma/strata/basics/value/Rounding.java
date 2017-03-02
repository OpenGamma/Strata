/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A convention defining how to round a number.
 * <p>
 * This defines a standard mechanism for rounding a {@code double} or {@link BigDecimal}.
 * Since financial instruments have different and complex conventions, rounding is extensible.
 * <p>
 * Note that rounding a {@code double} is not straightforward as floating point
 * numbers are based on a binary representation, not a decimal one.
 * For example, the value 0.1 cannot be exactly represented in a {@code double}.
 * <p>
 * The standard implementation is {@link HalfUpRounding}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface Rounding {

  /**
   * Obtains an instance that performs no rounding.
   * 
   * @return the rounding convention
   */
  public static Rounding none() {
    return NoRounding.INSTANCE;
  }

  /**
   * Obtains an instance that rounds to the specified number of decimal places.
   * <p>
   * This returns a convention that rounds to the specified number of decimal places.
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * 
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @return the rounding convention
   * @throws IllegalArgumentException if the decimal places is invalid
   */
  public static Rounding ofDecimalPlaces(int decimalPlaces) {
    return HalfUpRounding.ofDecimalPlaces(decimalPlaces);
  }

  /**
   * Obtains an instance from the number of decimal places and fraction.
   * <p>
   * This returns a convention that rounds to a fraction of the specified number of decimal places.
   * Rounding follows the normal {@link RoundingMode#HALF_UP} convention.
   * <p>
   * For example, to round to the nearest 1/32nd of the 4th decimal place, call
   * this method with the arguments 4 and 32.
   * 
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @param fraction  the fraction of the last decimal place, such as 32 for 1/32, from 0 to 255 inclusive
   * @return the rounding convention
   * @throws IllegalArgumentException if the decimal places or fraction is invalid
   */
  public static Rounding ofFractionalDecimalPlaces(int decimalPlaces, int fraction) {
    return HalfUpRounding.ofFractionalDecimalPlaces(decimalPlaces, fraction);
  }

  //-------------------------------------------------------------------------
  /**
   * Rounds the specified value according to the rules of the convention.
   * 
   * @param value  the value to be rounded
   * @return the rounded value
   */
  public default double round(double value) {
    return round(BigDecimal.valueOf(value)).doubleValue();
  }

  /**
   * Rounds the specified value according to the rules of the convention.
   * 
   * @param value  the value to be rounded
   * @return the rounded value
   */
  public abstract BigDecimal round(BigDecimal value);

}
