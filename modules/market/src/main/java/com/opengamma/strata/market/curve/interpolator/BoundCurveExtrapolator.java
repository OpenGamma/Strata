/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A curve extrapolator that has been bound to a specific curve.
 * <p>
 * A bound extrapolator is created from a {@link CurveExtrapolator}.
 * The bind process takes the definition of the extrapolator and combines it with the x-y values.
 * This allows implementations to optimize extrapolation calculations.
 * <p>
 * This interface is primarily used internally. Applications typically do not invoke these methods.
 */
public interface BoundCurveExtrapolator {

  /**
   * Left extrapolates the y-value from the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is less than the x-value of the first node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the y-value cannot be calculated
   */
  public abstract double leftExtrapolate(double xValue);

  /**
   * Calculates the first derivative of the left extrapolated y-value at the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is less than the x-value of the first node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the first derivative of the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double leftExtrapolateFirstDerivative(double xValue);

  /**
   * Calculates the parameter sensitivities of the left extrapolated y-value at the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is less than the x-value of the first node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the parameter sensitivities of the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract DoubleArray leftExtrapolateParameterSensitivity(double xValue);

  //-------------------------------------------------------------------------
  /**
   * Right extrapolates the y-value from the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is greater than the x-value of the last node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the y-value cannot be calculated
   */
  public abstract double rightExtrapolate(double xValue);

  /**
   * Calculates the first derivative of the right extrapolated y-value at the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is greater than the x-value of the last node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the first derivative of the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double rightExtrapolateFirstDerivative(double xValue);

  /**
   * Calculates the parameter sensitivities of the right extrapolated y-value at the specified x-value.
   * <p>
   * This method is only intended to be invoked when the x-value is greater than the x-value of the last node.
   * The behavior is undefined if called with any other x-value.
   * 
   * @param xValue  the x-value to find the y-value for
   * @return the parameter sensitivities of the extrapolated y-value for the specified x-value
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract DoubleArray rightExtrapolateParameterSensitivity(double xValue);

}
