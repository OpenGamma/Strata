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
import com.opengamma.strata.math.impl.interpolation.ClampedPiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.LogNaturalSplineHelper;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Log natural cubic spline interpolator for discount factors.
 * <p>
 * Find a interpolant F(x) = exp( f(x) ) where f(x) is a Natural cubic spline with Monotonicity cubic filter.
 * <p>
 * The natural cubic spline is determined by {@link LogNaturalSplineHelper}, where the tridiagonal
 * algorithm is used to solve a linear system.
 */
final class LogNaturalSplineDiscountFactorCurveInterpolator implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "LogNaturalSplineDiscountFactor";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new LogNaturalSplineDiscountFactorCurveInterpolator();

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
  private LogNaturalSplineDiscountFactorCurveInterpolator() {
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
    private final PiecewisePolynomialResultsWithSensitivity poly;
    private final DoubleArray knots;
    private final DoubleMatrix coefMatrix;
    private final int nKnots;
    private final int dimensions;
    private double[] logYValues;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.logYValues = getYLogValues(this.yValues);
      this.poly = new ClampedPiecewisePolynomialInterpolator(
          new NaturalSplineInterpolator(), new double[] {0d}, new double[] {0d})
              .interpolateWithSensitivity(xValues.toArray(), logYValues);
      this.knots = poly.getKnots();
      this.coefMatrix = poly.getCoefMatrix();
      this.nKnots = knots.size();
      this.dimensions = poly.getDimensions();
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.logYValues = base.logYValues;
      this.poly = base.poly;
      this.knots = base.knots;
      this.coefMatrix = base.coefMatrix;
      this.nKnots = base.nKnots;
      this.dimensions = base.dimensions;
    }

    //-------------------------------------------------------------------------
    private static double evaluate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
        int nKnots) {

      // check for 1 less interval than knots 
      int lowerBound = FunctionUtils.getLowerBoundIndex(knots, xValue);
      int indicator = lowerBound == nKnots - 1 ? lowerBound - 1 : lowerBound;
      DoubleArray coefs = coefMatrix.row(dimensions * indicator);
      return getValue(coefs.toArrayUnsafe(), xValue, knots.get(indicator));
    }

    private static double differentiate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
        int nKnots,
        int nCoefs,
        int numberOfIntervals) {

      int rowCount = dimensions * numberOfIntervals;
      int colCount = nCoefs - 1;
      DoubleMatrix coef = DoubleMatrix.of(
          rowCount,
          colCount,
          (i, j) -> coefMatrix.get(i, j) * (nCoefs - j - 1));
      return evaluate(xValue, knots, coef, dimensions, nKnots);
    }

    private static DoubleArray nodeSensitivity(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
        int nKnots,
        int interval,
        DoubleMatrix coefficientSensitivity) {

      double s = xValue - knots.get(interval);
      int nCoefs = coefficientSensitivity.rowCount();

      DoubleArray res = coefficientSensitivity.row(0);
      for (int i = 1; i < nCoefs; i++) {
        res = (DoubleArray) MA.scale(res, s);
        res = (DoubleArray) MA.add(res, coefficientSensitivity.row(i));
      }
      return res;
    }

    private static double[] getValues(double[] bareValues) {
      int nValues = bareValues.length;
      double[] res = new double[nValues];

      for (int i = 0; i < nValues; ++i) {
        res[i] = Math.exp(bareValues[i]);
      }
      return res;
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

    private static double[] getYLogValues(double[] yValues) {
      int nData = yValues.length;
      double[] logYValues = new double[nData];
      for (int i = 0; i < nData; ++i) {
        logYValues[i] = Math.log(yValues[i]);
      }
      return logYValues;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      double resValue = evaluate(xValue, knots, coefMatrix, dimensions, nKnots);
      return Math.exp(resValue);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      double resValue = evaluate(xValue, knots, coefMatrix, dimensions, nKnots);
      int nCoefs = poly.getOrder();
      int numberOfIntervals = poly.getNumberOfIntervals();
      double resDerivative = differentiate(xValue, knots, coefMatrix, dimensions, nKnots, nCoefs, numberOfIntervals);

      return Math.exp(resValue) * resDerivative;
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int interval = FunctionUtils.getLowerBoundIndex(knots, xValue);
      if (interval == nKnots - 1) {
        interval--; // there is 1 less interval that knots
      }

      DoubleMatrix coefficientSensitivity = poly.getCoefficientSensitivity(interval);
      double[] resSense = nodeSensitivity(
          xValue, knots, coefMatrix, dimensions, nKnots, interval, coefficientSensitivity).toArray();
      double resValue = Math.exp(evaluate(xValue, knots, coefMatrix, dimensions, nKnots));
      double[] knotValues = getValues(logYValues);
      final int knotValuesLength = knotValues.length;
      double[] res = new double[knotValuesLength];
      for (int i = 0; i < knotValuesLength; ++i) {
        res[i] = resSense[i + 1] * resValue / knotValues[i];
      }
      return DoubleArray.ofUnsafe(res);
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
