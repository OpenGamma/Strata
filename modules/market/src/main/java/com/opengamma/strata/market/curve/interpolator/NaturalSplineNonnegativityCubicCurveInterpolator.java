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
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.NonnegativityPreservingCubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Natural spline interpolator with non-negativity filter.
 */
final class NaturalSplineNonnegativityCubicCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "NaturalSplineNonnegativityCubic";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new NaturalSplineNonnegativityCubicCurveInterpolator();

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
  private NaturalSplineNonnegativityCubicCurveInterpolator() {
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

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      PiecewisePolynomialInterpolator underlying =
          new NonnegativityPreservingCubicSplineInterpolator(new NaturalSplineInterpolator());
      this.poly = underlying.interpolateWithSensitivity(xValues.toArray(), yValues.toArray());
      this.knots = poly.getKnots();
      this.coefMatrix = poly.getCoefMatrix();
      this.nKnots = knots.size();
      this.dimensions = poly.getDimensions();
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.poly = base.poly;
      this.knots = base.knots;
      this.coefMatrix = base.coefMatrix;
      this.nKnots = base.nKnots;
      this.dimensions = base.dimensions;
    }

    //-------------------------------------------------------------------------
    private static DoubleArray evaluate(
        double xValue,
        DoubleArray knots,
        DoubleMatrix coefMatrix,
        int dimensions,
        int nKnots) {

      // check for 1 less interval than knots 
      int lowerBound = FunctionUtils.getLowerBoundIndex(knots, xValue);
      int indicator = lowerBound == nKnots - 1 ? lowerBound - 1 : lowerBound;

      DoubleArray resArray = DoubleArray.of(dimensions, i -> {
        DoubleArray coefs = coefMatrix.row(dimensions * indicator + i);
        double res = getValue(coefs.toArrayUnsafe(), xValue, knots.get(indicator));
        return res;
      });
      return resArray;
    }

    private static DoubleArray differentiate(
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
      DoubleArray resValue = evaluate(xValue, knots, coefMatrix, dimensions, nKnots);
      return resValue.get(0);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int nCoefs = poly.getOrder();
      int numberOfIntervals = poly.getNumberOfIntervals();
      DoubleArray resValue = differentiate(xValue, knots, coefMatrix, dimensions, nKnots, nCoefs, numberOfIntervals);
      return resValue.get(0);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int interval = FunctionUtils.getLowerBoundIndex(knots, xValue);
      if (interval == nKnots - 1) {
        interval--; // there is 1 less interval than knots
      }
      DoubleMatrix coefficientSensitivity = poly.getCoefficientSensitivity(interval);
      int nCoefs = coefficientSensitivity.rowCount();
      double s = xValue - knots.get(interval);
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
