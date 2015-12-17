/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;

/**
 * Piecewise polynomial interpolator clamped at specified points. 
 * <p>
 * The clamped points are regarded as 'normal' data points in the interpolation result, i.e., 
 * {@code PiecewisePolynomialResult} or {@code PiecewisePolynomialResultsWithSensitivity}.  
 * A consequence of this is, for example, that the coefficient sensitivities involve the sensitivities to clamped points.
 */
public class ClampedPiecewisePolynomialInterpolator extends PiecewisePolynomialInterpolator {
  private final PiecewisePolynomialInterpolator _baseMethod;
  private final double[] _xValuesClamped;
  private final double[] _yValuesClamped;

  /**
   * Construct the interpolator with clamped points.
   * 
   * @param baseMethod The base interpolator must be not be itself
   * @param xValuesClamped X values of the clamped points
   * @param yValuesClamped Y values of the clamped points
   */
  public ClampedPiecewisePolynomialInterpolator(
      PiecewisePolynomialInterpolator baseMethod,
      double[] xValuesClamped,
      double[] yValuesClamped) {
    ArgChecker.notNull(baseMethod, "method");
    ArgChecker.notEmpty(xValuesClamped, "xValuesClamped");
    ArgChecker.notEmpty(yValuesClamped, "yValuesClamped");
    ArgChecker.isFalse(baseMethod instanceof ProductPiecewisePolynomialInterpolator,
        "baseMethod should not be ProductPiecewisePolynomialInterpolator");
    int nExtraPoints = xValuesClamped.length;
    ArgChecker.isTrue(yValuesClamped.length == nExtraPoints,
        "xValuesClamped and yValuesClamped should be the same length");
    _baseMethod = baseMethod;
    _xValuesClamped = Arrays.copyOf(xValuesClamped, nExtraPoints);
    _yValuesClamped = Arrays.copyOf(yValuesClamped, nExtraPoints);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    double[][] xyValuesAll = getDataTotal(xValues, yValues);
    return _baseMethod.interpolate(xyValuesAll[0], xyValuesAll[1]);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[][] yValuesMatrix) {
    throw new UnsupportedOperationException("Use 1D interpolation method");
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(double[] xValues, double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    double[][] xyValuesAll = getDataTotal(xValues, yValues);
    return _baseMethod.interpolateWithSensitivity(xyValuesAll[0], xyValuesAll[1]);
  }

  @Override
  public PiecewisePolynomialInterpolator getPrimaryMethod() {
    return _baseMethod;
  }

  private double[][] getDataTotal(double[] xData, double[] yData) {
    int nExtraPoints = _xValuesClamped.length;
    int nData = xData.length;
    int nTotal = nExtraPoints + nData;
    double[] xValuesTotal = new double[nTotal];
    double[] yValuesTotal = new double[nTotal];
    System.arraycopy(xData, 0, xValuesTotal, 0, nData);
    System.arraycopy(yData, 0, yValuesTotal, 0, nData);
    System.arraycopy(_xValuesClamped, 0, xValuesTotal, nData, nExtraPoints);
    System.arraycopy(_yValuesClamped, 0, yValuesTotal, nData, nExtraPoints);
    DoubleArrayMath.sortPairs(xValuesTotal, yValuesTotal);
    return new double[][] {xValuesTotal, yValuesTotal };
  }

}
