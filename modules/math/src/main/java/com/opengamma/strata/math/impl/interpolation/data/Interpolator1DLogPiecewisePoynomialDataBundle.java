/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * Data bundle for PiecewisePolynomialInerpolator1D with log transformation, 
 * i.e., an interpolant is F(x) = exp( f(x) ) where f(x) is a piecewise polynomial function. 
 * 
 * Since this data bundle possesses the information on f(x) via _poly of {@link PiecewisePolynomialResultsWithSensitivity},
 * any values computed by {@link PiecewisePolynomialWithSensitivityFunction1D} should be exponentiated.
 * 
 * yValues of the breakpoint information are transformed by this class. 
 */
public class Interpolator1DLogPiecewisePoynomialDataBundle extends Interpolator1DPiecewisePoynomialDataBundle {

  /**
   * 
   * @param underlyingData Contains sorted data (x,y)
   * @param method {@link PiecewisePolynomialInterpolator}
   */
  public Interpolator1DLogPiecewisePoynomialDataBundle(final Interpolator1DDataBundle underlyingData, final PiecewisePolynomialInterpolator method) {
    super(underlyingData, method);
  }

  /**
   * @param underlyingData Contains sorted data (x,y)
   * @param method  {@link PiecewisePolynomialInterpolator}
   * @param leftCond  Condition on left endpoint
   * @param rightCond  Condition on right endpoint
   */
  public Interpolator1DLogPiecewisePoynomialDataBundle(final Interpolator1DDataBundle underlyingData, final PiecewisePolynomialInterpolator method, final double leftCond, final double rightCond) {
    super(underlyingData, method, leftCond, rightCond);
  }

  @Override
  public double[] getBreakPointsY() {
    final double[] bareY = super.getBreakPointsY();
    final int nKnots = bareY.length;
    final double[] res = new double[nKnots];

    for (int i = 0; i < nKnots; ++i) {
      res[i] = Math.exp(bareY[i]);
    }
    return res;
  }

  @Override
  public Double firstValue() {
    return Math.exp(super.firstValue());
  }

  @Override
  public Double get(final Double key) {
    return Math.exp(super.get(key));
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(final Double key) {
    final int index = getLowerBoundIndex(key);
    final double[] values = getValues();
    if (index == size() - 1) {
      return new InterpolationBoundedValues(index, getKeys()[index], values[index], null, null);
    }
    return new InterpolationBoundedValues(index, getKeys()[index], values[index], getKeys()[index + 1], values[index + 1]);
  }

  @Override
  public double[] getValues() {
    final double[] bareValues = super.getValues();
    final int nValues = bareValues.length;
    final double[] res = new double[nValues];

    for (int i = 0; i < nValues; ++i) {
      res[i] = Math.exp(bareValues[i]);
    }
    return res;
  }

  @Override
  public Double lastValue() {
    return Math.exp(super.lastValue());
  }

}
