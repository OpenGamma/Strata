/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;

/**
 * C1 cubic interpolation preserving monotonicity based on 
 * Fritsch, F. N.; Carlson, R. E. (1980) 
 * "Monotone Piecewise Cubic Interpolation", SIAM Journal on Numerical Analysis 17 (2): 238–246. 
 * Fritsch, F. N. and Butland, J. (1984)
 * "A method for constructing local monotone piecewise cubic interpolants", SIAM Journal on Scientific and Statistical Computing 5 (2): 300-304.
 * 
 * For interpolation without node sensitivity, use {@link PiecewiseCubicHermiteSplineInterpolator}
 */
public class PiecewiseCubicHermiteSplineInterpolatorWithSensitivity extends PiecewisePolynomialInterpolator {

  /** interpolator without sensitivity **/
  private static final PiecewiseCubicHermiteSplineInterpolator INTERP = new PiecewiseCubicHermiteSplineInterpolator();

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(double[] xValues, double[] yValues) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be more than 1");
    int nDataPts = xValues.length;
    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isTrue(Double.isFinite(xValues[i]), "xData is not finite");
      ArgChecker.isTrue(Double.isFinite(yValues[i]), "yData is not finite");
    }
    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);
    for (int i = 1; i < nDataPts; ++i) {
      ArgChecker.isFalse(xValuesSrt[i - 1] == xValuesSrt[i], "xValues should be distinct");
    }

    DoubleMatrix[] temp = solve(xValuesSrt, yValuesSrt);
    // check the matrices
    int n = temp.length;
    for (int k = 0; k < n; k++) {
      DoubleMatrix m = temp[k];
      int rows = m.rowCount();
      int cols = m.columnCount();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.get(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }
    // copy
    DoubleMatrix coefMatrix = temp[0];
    DoubleMatrix[] coefMatrixSense = new DoubleMatrix[n - 1];
    System.arraycopy(temp, 1, coefMatrixSense, 0, n - 1);

    return new PiecewisePolynomialResultsWithSensitivity(DoubleArray.copyOf(xValuesSrt), coefMatrix, 4, 1, coefMatrixSense);
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Coefficient matrix whose i-th row vector is {a3, a2, a1, a0} of f(x) = a3 * (x-x_i)^3 + a2 * (x-x_i)^2 +... for the i-th interval
   */
  private DoubleMatrix[] solve(double[] xValues, double[] yValues) {

    int n = xValues.length;
    double[][] coeff = new double[n - 1][4];
    double[] h = new double[n - 1];
    double[] delta = new double[n - 1];
    DoubleMatrix[] res = new DoubleMatrix[n];
    for (int i = 0; i < n - 1; ++i) {
      h[i] = xValues[i + 1] - xValues[i];
      delta[i] = (yValues[i + 1] - yValues[i]) / h[i];
    }

    if (n == 2) {
      coeff[0][2] = delta[0];
      coeff[0][3] = yValues[0];
      double[][] coeffSense = new double[4][2];
      coeffSense[2][1] = 1d / h[0];
      coeffSense[2][0] = -1d / h[0];
      coeffSense[3][0] = 1d;
      res[1] = DoubleMatrix.copyOf(coeffSense);
    } else {
      SlopeFinderResults temp = slopeFinder(h, delta, yValues);
      DoubleArray d = temp.getSlopes();
      double[][] dDy = temp.getSlopeJacobian().toArray();

      // form up the coefficient matrix
      for (int i = 0; i < n - 1; ++i) {
        coeff[i][0] = (d.get(i) - 2 * delta[i] + d.get(i + 1)) / h[i] / h[i]; // b
        coeff[i][1] = (3 * delta[i] - 2. * d.get(i) - d.get(i + 1)) / h[i]; // c
        coeff[i][2] = d.get(i);
        coeff[i][3] = yValues[i];
      }

      double[][] bDy = new double[n - 1][n];
      double[][] cDy = new double[n - 1][n];
      for (int i = 0; i < n - 1; i++) {
        double invH = 1 / h[i];
        double invH2 = invH * invH;
        double invH3 = invH * invH2;
        cDy[i][i] = -3 * invH2;
        cDy[i][i + 1] = 3 * invH2;
        bDy[i][i] = 2 * invH3;
        bDy[i][i + 1] = -2 * invH3;
        for (int j = 0; j < n; j++) {
          cDy[i][j] -= (2 * dDy[i][j] + dDy[i + 1][j]) * invH;
          bDy[i][j] += (dDy[i][j] + dDy[i + 1][j]) * invH2;
        }
      }

      // Now we have to pack this into an array of DoubleMatrix - my kingdom for a tensor class
      for (int k = 0; k < n - 1; k++) {
        double[][] coeffSense = new double[4][];
        coeffSense[0] = bDy[k];
        coeffSense[1] = cDy[k];
        coeffSense[2] = dDy[k];
        coeffSense[3] = new double[n];
        coeffSense[3][k] = 1.0;
        res[k + 1] = DoubleMatrix.copyOf(coeffSense);
      }
    }

    res[0] = DoubleMatrix.copyOf(coeff);
    return res;
  }

  private class SlopeFinderResults {
    private final DoubleArray _d;
    private final DoubleMatrix _dDy;

    public SlopeFinderResults(DoubleArray d, DoubleMatrix dDy) {
      // this is a private class - don't do the normal checks on inputs
      _d = d;
      _dDy = dDy;
    }

    public DoubleArray getSlopes() {
      return _d;
    }

    public DoubleMatrix getSlopeJacobian() {
      return _dDy;
    }

  }

  /**
   * Finds the first derivatives at knots and their sensitivity to delta
   * @param h 
   * @param delta 
   * @return slope finder results 
   */
  private SlopeFinderResults slopeFinder(double[] h, double[] delta, double[] y) {
    final int n = y.length;

    double[] invDelta = new double[n - 1];
    double[] invDelta2 = new double[n - 1];
    double[] invH = new double[n - 1];
    for (int i = 0; i < (n - 1); i++) {
      invDelta[i] = 1 / delta[i];
      invDelta2[i] = invDelta[i] * invDelta[i];
      invH[i] = 1 / h[i];
    }

    double[] d = new double[n];
    double[][] jac = new double[n][n];

    // internal points
    for (int i = 1; i < n - 1; ++i) {
      if (delta[i] * delta[i - 1] > 0.) {
        double w1 = 2. * h[i] + h[i - 1];
        double w2 = h[i] + 2. * h[i - 1];
        double w12 = w1 + w2;
        d[i] = w12 / (w1 * invDelta[i - 1] + w2 * invDelta[i]);

        double z1 = d[i] * d[i] / w12;
        jac[i][i - 1] = -w1 * invH[i - 1] * invDelta2[i - 1] * z1;
        jac[i][i] = (w1 * invH[i - 1] * invDelta2[i - 1] - w2 * invH[i] * invDelta2[i]) * z1;
        jac[i][i + 1] = w2 * invH[i] * invDelta2[i] * z1;
      } else if (delta[i] == 0 ^ delta[i - 1] == 0) {
        // d is zero, so we don't explicitly set it
        double w1 = 2. * h[i] + h[i - 1];
        double w2 = h[i] + 2. * h[i - 1];
        double w12 = w1 + w2;
        double z2 = 0.5 * w12 / FunctionUtils.square(w1 * delta[i] + w2 * delta[i - 1]);
        jac[i][i - 1] = -w1 * invH[i - 1] * delta[i] * delta[i] * z2;
        jac[i][i] = (w1 * invH[i - 1] * delta[i] * delta[i] - w2 * invH[i] * delta[i - 1] * delta[i - 1]) * z2;
        jac[i][i + 1] = w2 * invH[i] * delta[i - 1] * delta[i - 1] * z2;
      }
    }

    // fill in end points
    double[] temp = endpointSlope(h[0], h[1], delta[0], delta[1], false);
    d[0] = temp[0];
    for (int i = 0; i < 3; i++) {
      jac[0][i] = temp[i + 1];
    }
    temp = endpointSlope(h[n - 2], h[n - 3], delta[n - 2], delta[n - 3], true);
    d[n - 1] = temp[0];
    for (int i = 1; i < 4; i++) {
      jac[n - 1][n - i] = temp[i];
    }

    return new SlopeFinderResults(DoubleArray.copyOf(d), DoubleMatrix.copyOf(jac));
  }

  /**
   * First derivative at end point and its sensitivity to delta
   * @param h1
   * @param h2
   * @param y1
   * @param y2
   * @param y3
   * @return array of length 4 - the first element contains d, while the other three are sensitivities to the ys 
   */
  private double[] endpointSlope(double h1, double h2, double del1, double del2, boolean rightSide) {

    double[] res = new double[4];

    if (del1 == 0.0) { // quick exist for particular edge case
      // d and dDy3 are both zero - no need to explicitly set
      if (del2 == 0) {
        res[1] = -(2 * h1 + h2) / h1 / (h1 + h2);
        if (h1 > 2 * h2) {
          res[2] = 3 / h1;
        } else {
          res[2] = (h1 + h2) / h1 / h2;
        }
      } else {
        res[1] = -1.5 / h1;
        res[2] = -res[1];
      }
      if (rightSide) {
        res[1] = -res[1];
        res[2] = -res[2];
      }
      return res;
    }

    // This value is used in the clauses - may not be the returned value
    double d = ((2. * h1 + h2) * del1 - h1 * del2) / (h1 + h2);

    if (Math.signum(d) != Math.signum(del1)) {
      // again d is now set to zero
      if (Math.abs(d) < 1e-15) {
        res[1] = -(2 * h1 + h2) / h1 / (h1 + h2);
        res[2] = (h1 + h2) / h1 / h2;
        res[3] = -h1 / h2 / (h1 + h2);
      }
    } else if (Math.signum(del1) != Math.signum(del2) && Math.abs(d) > 3. * Math.abs(del1)) {
      res[0] = 3 * del1;
      res[1] = -3 / h1;
      res[2] = -res[1];
    } else {
      res[0] = d;
      res[1] = -(2 * h1 + h2) / h1 / (h1 + h2);
      res[2] = (h1 + h2) / h1 / h2;
      res[3] = -h1 / h2 / (h1 + h2);
    }

    if (rightSide) {
      for (int i = 1; i < 4; i++) {
        res[i] = -res[i];
      }
    }
    return res;
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {
    return INTERP.interpolate(xValues, yValues);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[][] yValuesMatrix) {
    return INTERP.interpolate(xValues, yValuesMatrix);
  }

}
