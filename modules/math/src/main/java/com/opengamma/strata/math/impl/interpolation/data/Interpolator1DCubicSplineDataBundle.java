/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import java.util.Objects;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.InverseTridiagonalMatrixCalculator;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalMatrix;

/**
 * 
 */
public class Interpolator1DCubicSplineDataBundle
    extends ForwardingInterpolator1DDataBundle {

  private double[] _secondDerivatives;
  private double[][] _secondDerivativesSensitivities;
  private final double _leftFirstDev;
  private final double _rightFirstDev;
  private final boolean _leftNatural;
  private final boolean _rightNatural;

  public Interpolator1DCubicSplineDataBundle(Interpolator1DDataBundle underlyingData) {
    super(underlyingData);
    _leftFirstDev = 0;
    _rightFirstDev = 0;
    _leftNatural = true;
    _rightNatural = true;
  }

  /**
   * Data bundle for a cubic spline 
   * @param underlyingData the data
   * @param leftGrad The gradient of the function at the left most knot. <b>Note: </b>to leave this unspecified (i.e. natural with zero second derivative),
   *  set the value to Double.POSITIVE_INFINITY
   * @param rightGrad The gradient of the function at the right most knot. <b>Note: </b>to leave this unspecified (i.e. natural with zero second derivative),
   *  set the value to Double.POSITIVE_INFINITY
   */
  public Interpolator1DCubicSplineDataBundle(Interpolator1DDataBundle underlyingData, double leftGrad, double rightGrad) {
    super(underlyingData);
    if (Double.isInfinite(leftGrad)) {
      _leftFirstDev = 0;
      _leftNatural = true;
    } else {
      _leftFirstDev = leftGrad;
      _leftNatural = false;
    }
    if (Double.isInfinite(rightGrad)) {
      _rightFirstDev = 0;
      _rightNatural = true;
    } else {
      _rightFirstDev = leftGrad;
      _rightNatural = false;
    }
  }

  private double[] calculateSecondDerivative() {
    double[] x = getKeys();
    double[] y = getValues();
    int n = x.length;
    double[] deltaX = new double[n - 1];
    double[] deltaYOverDeltaX = new double[n - 1];
    double[] oneOverDeltaX = new double[n - 1];

    for (int i = 0; i < n - 1; i++) {
      deltaX[i] = x[i + 1] - x[i];
      oneOverDeltaX[i] = 1.0 / deltaX[i];
      deltaYOverDeltaX[i] = (y[i + 1] - y[i]) * oneOverDeltaX[i];
    }
    DoubleMatrix inverseTriDiag = getInverseTridiagonalMatrix(deltaX);
    DoubleArray rhsVector = getRHSVector(deltaYOverDeltaX);
    return ((DoubleArray) OG_ALGEBRA.multiply(inverseTriDiag, rhsVector)).toArray();
  }

  public double[] getSecondDerivatives() {
    if (_secondDerivatives == null) {
      _secondDerivatives = calculateSecondDerivative();
    }
    return _secondDerivatives;
  }

  //TODO not ideal that it recomputes the inverse matrix
  public double[][] getSecondDerivativesSensitivities() {
    if (_secondDerivativesSensitivities == null) {
      double[] x = getKeys();
      double[] y = getValues();
      int n = x.length;
      double[] deltaX = new double[n - 1];
      double[] deltaYOverDeltaX = new double[n - 1];
      double[] oneOverDeltaX = new double[n - 1];

      for (int i = 0; i < n - 1; i++) {
        deltaX[i] = x[i + 1] - x[i];
        oneOverDeltaX[i] = 1.0 / deltaX[i];
        deltaYOverDeltaX[i] = (y[i + 1] - y[i]) * oneOverDeltaX[i];
      }

      DoubleMatrix inverseTriDiag = getInverseTridiagonalMatrix(deltaX);
      DoubleMatrix rhsMatrix = getRHSMatrix(oneOverDeltaX);
      _secondDerivativesSensitivities = ((DoubleMatrix) OG_ALGEBRA.multiply(inverseTriDiag, rhsMatrix)).toArray();
    }
    return _secondDerivativesSensitivities;
  }

  private DoubleMatrix getRHSMatrix(double[] oneOverDeltaX) {
    int n = oneOverDeltaX.length + 1;

    double[][] res = new double[n][n];
    for (int i = 1; i < n - 1; i++) {
      res[i][i - 1] = oneOverDeltaX[i - 1];
      res[i][i] = -oneOverDeltaX[i] - oneOverDeltaX[i - 1];
      res[i][i + 1] = oneOverDeltaX[i];
    }
    if (!_leftNatural) {
      res[0][0] = oneOverDeltaX[0];
      res[0][1] = -oneOverDeltaX[0];
    }

    if (!_rightNatural) {
      res[n - 1][n - 1] = -oneOverDeltaX[n - 2];
      res[n - 2][n - 2] = oneOverDeltaX[n - 2];
    }
    return DoubleMatrix.copyOf(res);
  }

  private DoubleArray getRHSVector(double[] deltaYOverDeltaX) {
    int n = deltaYOverDeltaX.length + 1;
    double[] res = new double[n];

    for (int i = 1; i < n - 1; i++) {
      res[i] = deltaYOverDeltaX[i] - deltaYOverDeltaX[i - 1];
    }
    if (!_leftNatural) {
      res[0] = _leftFirstDev - deltaYOverDeltaX[0];
    }

    if (!_rightNatural) {
      res[n - 1] = _rightFirstDev - deltaYOverDeltaX[n - 2];
    }
    return DoubleArray.copyOf(res);
  }

  private DoubleMatrix getInverseTridiagonalMatrix(double[] deltaX) {
    InverseTridiagonalMatrixCalculator invertor = new InverseTridiagonalMatrixCalculator();
    int n = deltaX.length + 1;
    double[] a = new double[n];
    double[] b = new double[n - 1];
    double[] c = new double[n - 1];
    for (int i = 1; i < n - 1; i++) {
      a[i] = (deltaX[i - 1] + deltaX[i]) / 3.0;
      b[i] = deltaX[i] / 6.0;
      c[i - 1] = deltaX[i - 1] / 6.0;
    }
    // Boundary condition
    if (_leftNatural) {
      a[0] = 1.0;
      b[0] = 0.0;
    } else {
      a[0] = -deltaX[0] / 3.0;
      b[0] = deltaX[0] / 6.0;
    }
    if (_rightNatural) {
      a[n - 1] = 1.0;
      c[n - 2] = 0.0;
    } else {
      a[n - 1] = deltaX[n - 2] / 3.0;
      c[n - 2] = deltaX[n - 2] / 6.0;
    }

    TridiagonalMatrix tridiagonal = new TridiagonalMatrix(a, b, c);
    return invertor.apply(tridiagonal);
  }

  @Override
  public void setYValueAtIndex(int index, double y) {
    ArgChecker.notNegative(index, "index");
    if (index >= size()) {
      throw new IllegalArgumentException("Index was greater than number of data points");
    }
    getUnderlying().setYValueAtIndex(index, y);
    _secondDerivatives = null;
    _secondDerivativesSensitivities = null;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_leftFirstDev);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + 1237;
    temp = Double.doubleToLongBits(_rightFirstDev);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + 1237;
    result = prime * result + ((getUnderlying() == null) ? 0 : getUnderlying().hashCode());
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
    if (getClass() != obj.getClass()) {
      return false;
    }
    Interpolator1DCubicSplineDataBundle other = (Interpolator1DCubicSplineDataBundle) obj;
    if (!Objects.equals(getUnderlying(), other.getUnderlying())) {
      return false;
    }
    if (Double.doubleToLongBits(_leftFirstDev) != Double.doubleToLongBits(other._leftFirstDev)) {
      return false;
    }
    if (_leftNatural != other._leftNatural) {
      return false;
    }
    if (Double.doubleToLongBits(_rightFirstDev) != Double.doubleToLongBits(other._rightFirstDev)) {
      return false;
    }
    if (_rightNatural != other._rightNatural) {
      return false;
    }
    return true;
  }

}
