/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.interpolation.LogNaturalSplineHelper;
import com.opengamma.strata.math.impl.interpolation.MonotonicityPreservingCubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Log natural cubic interpolation with monotonicity filter.
 * <p>
 * Finds an interpolant {@code F(x) = exp( f(x) )} where {@code f(x)} is a Natural cubic
 * spline with Monotonicity cubic filter.
 */
final class LogNaturalSplineMonotoneCubicInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "LogNaturalSplineMonotoneCubic";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new LogNaturalSplineMonotoneCubicInterpolator();

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
  private LogNaturalSplineMonotoneCubicInterpolator() {
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
    private final PiecewisePolynomialResult poly;
    private final Supplier<PiecewisePolynomialResultsWithSensitivity> polySens;
    private double[] logYValues;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.logYValues = getYLogValues(this.yValues);
      PiecewisePolynomialInterpolator underlying =
          new MonotonicityPreservingCubicSplineInterpolator(new LogNaturalSplineHelper());
      this.poly = underlying.interpolate(xValues.toArray(), logYValues);
      this.polySens = Suppliers.memoize(() -> underlying.interpolateWithSensitivity(xValues.toArray(), logYValues));
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.logYValues = base.logYValues;
      this.poly = base.poly;
      this.polySens = base.polySens;
    }

    //-------------------------------------------------------------------------
    private static double evaluate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions) {

      // check for 1 less interval than knots 
      int lowerBound = FunctionUtils.getLowerBoundIndex(knots, xValue);
      int indicator = lowerBound == knots.size() - 1 ? lowerBound - 1 : lowerBound;
      DoubleArray coefs = coefMatrix.row(dimensions * indicator);
      return getValue(coefs.toArrayUnsafe(), xValue, knots.get(indicator));
    }

    private static double differentiate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
        int nCoefs,
        int numberOfIntervals) {

      int rowCount = dimensions * numberOfIntervals;
      int colCount = nCoefs - 1;
      DoubleMatrix coef = DoubleMatrix.of(
          rowCount,
          colCount,
          (i, j) -> coefMatrix.get(i, j) * (nCoefs - j - 1));
      return evaluate(xValue, knots, coef, dimensions);
    }

    private static DoubleArray nodeSensitivity(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
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
      double resValue = evaluate(xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions());
      return Math.exp(resValue);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      double resValue = evaluate(xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions());
      int nCoefs = poly.getOrder();
      int numberOfIntervals = poly.getNumberOfIntervals();
      double resDerivative = differentiate(
          xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions(), nCoefs, numberOfIntervals);

      return Math.exp(resValue) * resDerivative;
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int interval = FunctionUtils.getLowerBoundIndex(poly.getKnots(), xValue);
      if (interval == poly.getKnots().size() - 1) {
        interval--; // there is 1 less interval that knots
      }

      DoubleMatrix coefficientSensitivity = polySens.get().getCoefficientSensitivity(interval);
      double[] resSense = nodeSensitivity(
          xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions(), interval, coefficientSensitivity).toArray();
      double resValue = Math.exp(evaluate(xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions()));
      double[] knotValues = getValues(logYValues);
      final int knotValuesLength = knotValues.length;
      double[] res = new double[knotValuesLength];
      for (int i = 0; i < knotValuesLength; ++i) {
        res[i] = resSense[i] * resValue / knotValues[i];
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
