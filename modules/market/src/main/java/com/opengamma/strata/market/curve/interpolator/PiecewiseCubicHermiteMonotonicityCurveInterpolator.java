/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.PiecewiseCubicHermiteSplineInterpolatorWithSensitivity;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Cubic Hermite interpolation preserving monotonicity.
 * <p>
 * The data points are interpolated by piecewise cubic Heremite polynomials. 
 * The interpolation functions are monotonic in each interval.
 */
final class PiecewiseCubicHermiteMonotonicityCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "PiecewiseCubicHermiteMonotonicity";
  /**
   * The interpolator instance.
   */
  public static final PiecewiseCubicHermiteMonotonicityCurveInterpolator INSTANCE =
      new PiecewiseCubicHermiteMonotonicityCurveInterpolator();

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Underlying matrix algebra.
   */
  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  /**
   * Restricted constructor.
   */
  private PiecewiseCubicHermiteMonotonicityCurveInterpolator() {
  }

  // resolve instance
  private Object readResolve() {
    return INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public BoundCurveInterpolator bind(DoubleArray xValues, DoubleArray yValues) {
    return new Bound(xValues, yValues);
  }

  //-----------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound interpolator.
   */
  static class Bound extends AbstractBoundCurveInterpolator {
    private final double[] xValues;
    private final double[] yValues;
    private final DoubleArray knots;
    private final DoubleMatrix coefMatrix;
    private final DoubleMatrix[] coefMatrixSensi;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      PiecewisePolynomialInterpolator underlying = new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
      PiecewisePolynomialResultsWithSensitivity poly =
          underlying.interpolateWithSensitivity(xValues.toArray(), yValues.toArray());
      this.knots = poly.getKnots();
      this.coefMatrix = poly.getCoefMatrix();
      this.coefMatrixSensi = poly.getCoefficientSensitivityAll();
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.knots = base.knots;
      this.coefMatrix = base.coefMatrix;
      this.coefMatrixSensi = base.coefMatrixSensi;
    }

    //-------------------------------------------------------------------------
    private static double evaluate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix) {

      int indicator = getIndicator(xValue, knots);
      DoubleArray coefs = coefMatrix.row(indicator);
      return getValue(coefs.toArrayUnsafe(), xValue, knots.get(indicator));
    }

    private static double differentiate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix) {

      int indicator = getIndicator(xValue, knots);
      DoubleArray coefs = coefMatrix.row(indicator);
      return getDerivativeValue(coefs.toArrayUnsafe(), xValue, knots.get(indicator));
    }

    private static int getIndicator(double xValue, DoubleArray knots) {
      // check for 1 less interval than knots 
      int lowerBound = FunctionUtils.getLowerBoundIndex(knots, xValue);
      return lowerBound == knots.size() - 1 ? lowerBound - 1 : lowerBound;
    }

    /**
     * @param coefs  {a_n,a_{n-1},...} of f(x) = a_n x^{n} + a_{n-1} x^{n-1} + ....
     * @param x  the x
     * @param leftknot  the knot specifying underlying interpolation function
     * @return the value of the underlying interpolation function at the value of x
     */
    private static double getValue(double[] coefs, double x, double leftknot) {
      int nCoefs = coefs.length;
      double s = x - leftknot;
      double res = coefs[0];
      for (int i = 1; i < nCoefs; i++) {
        res *= s;
        res += coefs[i];
      }
      return res;
    }

    /**
     * @param coefs  {a_n,a_{n-1},...} of f(x) = a_n x^{n} + a_{n-1} x^{n-1} + ....
     * @param x  the x
     * @param leftknot  the knot specifying underlying interpolation function
     * @return the value of the underlying interpolation function at the value of x
     */
    private static double getDerivativeValue(double[] coefs, double x, double leftknot) {
      int nCoefs = coefs.length;
      double s = x - leftknot;
      double res = (nCoefs - 1) * coefs[0];
      for (int i = 1; i < nCoefs - 1; i++) { // nCoefs > 1
        res *= s;
        res += (nCoefs - i - 1d) * coefs[i];
      }
      return res;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      return evaluate(xValue, knots, coefMatrix);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      return differentiate(xValue, knots, coefMatrix);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int indicator = getIndicator(xValue, knots);
      DoubleMatrix coefficientSensitivity = coefMatrixSensi[indicator];
      int nCoefs = coefficientSensitivity.rowCount();
      double s = xValue - knots.get(indicator);
      DoubleArray res = coefficientSensitivity.row(0);
      for (int i = 1; i < nCoefs; i++) {
        res = (DoubleArray) MA.scale(res, s);
        res = (DoubleArray) MA.add(res, coefficientSensitivity.row(i));
      }
      return res;
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
