/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * Data bundle for PiecewisePolynomialInterpolator1D
 */
public class Interpolator1DPiecewisePoynomialDataBundle
    extends ForwardingInterpolator1DDataBundle {

  private final PiecewisePolynomialResultsWithSensitivity _poly;

  /**
   * Constructor where coefficients for interpolant and its node sensitivity are computed 
   * @param underlyingData Contains sorted data (x,y)
   * @param method {@link PiecewisePolynomialInterpolator}
   */
  public Interpolator1DPiecewisePoynomialDataBundle(Interpolator1DDataBundle underlyingData, PiecewisePolynomialInterpolator method) {
    super(underlyingData);
    ArgChecker.notNull(method, "method");
    _poly = method.interpolateWithSensitivity(underlyingData.getKeys(), underlyingData.getValues());
  }

  /**
   * @param underlyingData Contains sorted data (x,y)
   * @param method  {@link PiecewisePolynomialInterpolator}
   * @param leftCond  Condition on left endpoint
   * @param rightCond  Condition on right endpoint
   */
  public Interpolator1DPiecewisePoynomialDataBundle(Interpolator1DDataBundle underlyingData, PiecewisePolynomialInterpolator method, double leftCond, double rightCond) {
    super(underlyingData);
    ArgChecker.notNull(method, "method");
    double[] yValues = underlyingData.getValues();
    int nData = yValues.length;
    double[] yValuesMod = new double[nData + 2];
    yValuesMod[0] = leftCond;
    yValuesMod[nData + 1] = rightCond;
    System.arraycopy(yValues, 0, yValuesMod, 1, nData);

    _poly = method.interpolateWithSensitivity(underlyingData.getKeys(), yValuesMod);
  }

  /**
   * Access PiecewisePolynomialResultsWithSensitivity
   * @return PiecewisePolynomialResultsWithSensitivity
   */
  public PiecewisePolynomialResultsWithSensitivity getPiecewisePolynomialResultsWithSensitivity() {
    return _poly;
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
    if (!(obj instanceof Interpolator1DPiecewisePoynomialDataBundle)) {
      return false;
    }
    Interpolator1DPiecewisePoynomialDataBundle other = (Interpolator1DPiecewisePoynomialDataBundle) obj;
    if (!getUnderlying().equals(other.getUnderlying())) {
      return false;
    }
    if (!_poly.equals(other._poly)) {
      return false;
    }
    return true;
  }
}
