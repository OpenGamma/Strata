/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Contains utility methods for maths on doubles that better simulate decimal math.
 * <p>
 * With floating point maths, {@code 0.1 + 0.2 != 0.3}.
 * This class handles cases like this by converting to {@code BigDecimal} and back.
 */
public final class DecimalMath {

  /**
   * An empty {@code double} array.
   */
  public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
  /**
   * An empty {@code Double} array.
   */
  public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];

  /**
   * Restricted constructor.
   */
  private DecimalMath() {
  }

  //-------------------------------------------------------------------------
  /**
   * Adds two {@code double} values.
   * 
   * @param base  the base value
   * @param amountToAdd  the value to add
   * @return the result
   */
  public static double add(double base, double amountToAdd) {
    try {
      return BigDecimal.valueOf(base).add(BigDecimal.valueOf(amountToAdd)).doubleValue();
    } catch (NumberFormatException ex) {
      return base + amountToAdd;
    }
  }

  /**
   * Subtracts one {@code double} value from another.
   * 
   * @param base  the base value
   * @param amountToSubtract  the value to subtract
   * @return the result
   */
  public static double subtract(double base, double amountToSubtract) {
    try {
      return BigDecimal.valueOf(base).subtract(BigDecimal.valueOf(amountToSubtract)).doubleValue();
    } catch (NumberFormatException ex) {
      return base - amountToSubtract;
    }
  }

  /**
   * Multiplies two {@code double} values.
   * 
   * @param base  the base value
   * @param amountToMultiplyBy  the amount to multiply by
   * @return the result
   */
  public static double multiply(double base, double amountToMultiplyBy) {
    try {
      return BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(amountToMultiplyBy)).doubleValue();
    } catch (NumberFormatException ex) {
      return base * amountToMultiplyBy;
    }
  }

  /**
   * Divides one {@code double} value from another.
   * 
   * @param base  the base value
   * @param amountToDivideBy  the amount to divide by
   * @return the result
   */
  public static double divide(double base, double amountToDivideBy) {
    try {
      return BigDecimal.valueOf(base).divide(BigDecimal.valueOf(amountToDivideBy), MathContext.DECIMAL128)
          .doubleValue();
    } catch (NumberFormatException ex) {
      return base / amountToDivideBy;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Multiplies two {@code double} values, then divides by a third.
   * 
   * @param base  the base value
   * @param amountToMultiplyBy  the amount to multiply by
   * @param amountToDivideBy  the amount to divide by
   * @return the result
   */
  public static double multiplyDivide(double base, double amountToMultiplyBy, double amountToDivideBy) {
    try {
      return BigDecimal.valueOf(base)
          .multiply(BigDecimal.valueOf(amountToMultiplyBy))
          .divide(BigDecimal.valueOf(amountToDivideBy), MathContext.DECIMAL128)
          .doubleValue();
    } catch (NumberFormatException ex) {
      return (base * amountToMultiplyBy) / amountToDivideBy;
    }
  }

  /**
   * Multiplies two {@code double} values, then adds a third.
   * 
   * @param base  the base value
   * @param amountToMultiplyBy  the amount to multiply by
   * @param amountToAdd  the amount to add
   * @return the result
   */
  public static double multiplyAdd(double base, double amountToMultiplyBy, double amountToAdd) {
    try {
      return BigDecimal.valueOf(base)
          .multiply(BigDecimal.valueOf(amountToMultiplyBy))
          .add(BigDecimal.valueOf(amountToAdd))
          .doubleValue();
    } catch (NumberFormatException ex) {
      return (base * amountToMultiplyBy) + amountToAdd;
    }
  }

}
