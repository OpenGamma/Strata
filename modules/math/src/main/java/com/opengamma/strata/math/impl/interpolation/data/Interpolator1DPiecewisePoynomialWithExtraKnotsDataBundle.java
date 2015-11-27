/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * For certain methods of {@link PiecewisePolynomialInterpolator} introducing extra breakpoints, {@link PiecewisePolynomialResultsWithSensitivity} is not well-defined
 * In this case, finite difference approximation is used to derive node sensitivity
 */
public class Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle
    extends ForwardingInterpolator1DDataBundle {

  private final PiecewisePolynomialResult _poly;
  private final PiecewisePolynomialResult[] _polyUp;
  private final PiecewisePolynomialResult[] _polyDw;

  private static final double EPS = 1.e-7;
  private static final double SMALL = 1.e-14;

  /**
   * Constructor where coefficients for interpolant and its node sensitivity are computed 
   * @param underlyingData Contains sorted data (x,y)
   * @param method {@link PiecewisePolynomialInterpolator}
   */
  public Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle(Interpolator1DDataBundle underlyingData, PiecewisePolynomialInterpolator method) {
    super(underlyingData);
    ArgChecker.notNull(method, "method");

    _poly = method.interpolate(underlyingData.getKeys(), underlyingData.getValues());

    double[] yValues = underlyingData.getValues();
    int nData = yValues.length;
    _polyUp = new PiecewisePolynomialResult[nData];
    _polyDw = new PiecewisePolynomialResult[nData];
    double[] yValuesUp = Arrays.copyOf(yValues, nData);
    double[] yValuesDw = Arrays.copyOf(yValues, nData);
    for (int i = 0; i < nData; ++i) {
      yValuesUp[i] = Math.abs(yValues[i]) < SMALL ? EPS : yValues[i] * (1. + EPS);
      yValuesDw[i] = Math.abs(yValues[i]) < SMALL ? -EPS : yValues[i] * (1. - EPS);
      _polyUp[i] = method.interpolate(underlyingData.getKeys(), yValuesUp);
      _polyDw[i] = method.interpolate(underlyingData.getKeys(), yValuesDw);
      yValuesUp[i] = yValues[i];
      yValuesDw[i] = yValues[i];
    }
  }

  /**
   * Access PiecewisePolynomialResult
   * @return PiecewisePolynomialResult
   */
  public PiecewisePolynomialResult getPiecewisePolynomialResult() {
    return _poly;
  }

  /**
   * Access PiecewisePolynomialResult with yValuesUp
   * @return PiecewisePolynomialResult
   */
  public PiecewisePolynomialResult[] getPiecewisePolynomialResultUp() {
    return _polyUp;
  }

  /**
   * Access PiecewisePolynomialResult with yValuesDw
   * @return PiecewisePolynomialResult
   */
  public PiecewisePolynomialResult[] getPiecewisePolynomialResultDw() {
    return _polyDw;
  }

  /**
   * Access a fixed parameter for the finite difference approximation
   * @return EPS
   */
  public double getEps() {
    return EPS;
  }

  /**
   * Access a fixed parameter for the finite difference approximation
   * @return SMALL
   */
  public double getSmall() {
    return SMALL;
  }

  /**
   * Get x values of breakpoints, which are different from "keys" for certain interpolations
   * @return X values of breakpoints
   */
  public double[] getBreakpointsX() {
    return _poly.getKnots().toArray();
  }

  /**
   * Get y values of breakpoints, which are different from "values" for certain interpolations
   * @return Y values of breakpoints
   */
  public double[] getBreakPointsY() {
    int nKnots = _poly.getKnots().size();
    DoubleMatrix coefMat = _poly.getCoefMatrix();
    int nCoefs = coefMat.columnCount();
    double[] values = new double[nKnots];
    for (int i = 0; i < nKnots - 1; i++) {
      values[i] = coefMat.get(i, nCoefs - 1);
    }
    values[nKnots - 1] = getUnderlying().lastValue();

    return values;
  }

  @Override
  public void setYValueAtIndex(int index, double y) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + _poly.hashCode();
    result = prime * result + getUnderlying().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle)) {
      return false;
    }
    Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle other = (Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle) obj;
    if (!getUnderlying().equals(other.getUnderlying())) {
      return false;
    }
    if (!_poly.equals(other._poly)) {
      return false;
    }
    return true;
  }

}
