/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

/**
 * Performs a parallel shift on a set of curve data if the curve matches the {@link CurveSpecificationMatcher matcher}.
 */
public class CurveDataParallelShift extends CurveDataShift {

  /**
   * Creates a shift that adds an absolute amount to each market data point in the curve.
   *
   * @param shiftAmount the amount to add to each point
   * @param matcher for deciding whether a curve should be shifted
   */
  public CurveDataParallelShift(double shiftAmount, CurveSpecificationMatcher matcher) {
    super(shiftAmount, matcher);
  }

  /**
   * Shifts a single value. Only invoked if the curve matches the matcher. If the curve data is quoted the other
   * way up (e.g. for futures) it is inverted before calling this method.
   *
   * @param normalizedValue the value
   * @return the shifted value
   */
  protected double shift(double normalizedValue) {
    return normalizedValue + getShiftAmount();
  }
}
