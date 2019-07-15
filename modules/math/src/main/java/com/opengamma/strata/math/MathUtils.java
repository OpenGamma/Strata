/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math;

/**
 * Simple utilities for maths.
 */
public final class MathUtils {

  /**
   * Returns the power of 2 (square).
   * 
   * @param value  the value to check
   * @return {@code value * value}
   */
  public static double pow2(double value) {
    return value * value;
  }

  /**
   * Returns the power of 3 (cube).
   * 
   * @param value  the value to check
   * @return {@code value * value * value}
   */
  public static double pow3(double value) {
    return value * value * value;
  }

  /**
   * Returns the power of 4.
   * 
   * @param value  the value to check
   * @return {@code value * value * value * value}
   */
  public static double pow4(double value) {
    return value * value * value * value;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if a number is near zero.
   * <p>
   * NaN and infinity are not near zero.
   * 
   * @param value  the value to check
   * @param tolerance  the tolerance, must be positive and not NaN/Infinite (not validated)
   * @return true if near zero
   */
  public static boolean nearZero(double value, double tolerance) {
    return value >= -tolerance && value <= tolerance;
  }

  /**
   * Checks if a number is near one.
   * <p>
   * NaN and infinity are not near one.
   * 
   * @param value  the value to check
   * @param tolerance  the tolerance, must be positive and not NaN/Infinite (not validated)
   * @return true if near one
   */
  public static boolean nearOne(double value, double tolerance) {
    return value >= 1 - tolerance && value <= 1 + tolerance;
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private MathUtils() {
  }

}
