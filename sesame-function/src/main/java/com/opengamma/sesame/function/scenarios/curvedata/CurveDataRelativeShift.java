/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

/**
 * Performs a relative shift on a set of curve input data if the curve matches a {@link CurveSpecificationMatcher matcher}.
 */
public class CurveDataRelativeShift extends CurveDataShift {

  /**
   * Creates a shift that adds a relative amount to each market data point in the curve.
   * A shift of 0.1 (+10%) scales the point value by 1.1, a shift of -0.2 (-20%) scales the point value by 0.8.
   *
   * @param shiftAmount the amount to add to each point
   * @param matcher for deciding whether a curve should be shifted
   */
  public CurveDataRelativeShift(double shiftAmount, CurveSpecificationMatcher matcher) {
    super(1 + shiftAmount, matcher);
  }

  @Override
  protected double shift(double normalizedValue) {
    return normalizedValue * getShiftAmount();
  }
}
