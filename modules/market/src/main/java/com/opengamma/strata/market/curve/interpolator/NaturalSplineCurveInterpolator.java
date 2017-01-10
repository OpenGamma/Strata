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
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Natural spline interpolator.
 */
final class NaturalSplineCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "NaturalSpline";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new NaturalSplineCurveInterpolator();

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
  private NaturalSplineCurveInterpolator() {
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

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      PiecewisePolynomialInterpolator underlying = new NaturalSplineInterpolator();
      this.poly = underlying.interpolate(xValues.toArray(), yValues.toArray());
      this.polySens = Suppliers.memoize(() -> underlying.interpolateWithSensitivity(xValues.toArray(), yValues.toArray()));
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
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

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      return evaluate(xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions());
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int nCoefs = poly.getOrder();
      int numberOfIntervals = poly.getNumberOfIntervals();
      return differentiate(xValue, poly.getKnots(), poly.getCoefMatrix(), poly.getDimensions(), nCoefs, numberOfIntervals);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int interval = FunctionUtils.getLowerBoundIndex(poly.getKnots(), xValue);
      if (interval == poly.getKnots().size() - 1) {
        interval--; // there is 1 less interval than knots
      }
      DoubleMatrix coefficientSensitivity = polySens.get().getCoefficientSensitivity(interval);
      int nCoefs = coefficientSensitivity.rowCount();
      double s = xValue - poly.getKnots().get(interval);
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
