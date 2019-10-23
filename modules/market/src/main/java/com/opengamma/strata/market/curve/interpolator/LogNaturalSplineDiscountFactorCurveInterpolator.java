/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.ClampedPiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.LogNaturalSplineHelper;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * Log natural cubic spline interpolator for discount factors.
 * <p>
 * Find a interpolant F(x) = exp( f(x) ) so that F(0) = 1 where f(x) is a Natural cubic spline.
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
   * The polynomial function.
   */
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNCTION = new PiecewisePolynomialWithSensitivityFunction1D();

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
    private final PiecewisePolynomialResult poly;
    private final Supplier<PiecewisePolynomialResultsWithSensitivity> polySens;
    private double[] logYValues;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.logYValues = getYLogValues(this.yValues);
      ClampedPiecewisePolynomialInterpolator underlying = new ClampedPiecewisePolynomialInterpolator(
          new NaturalSplineInterpolator(), new double[] {0d}, new double[] {0d});
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
      double resValue = FUNCTION.evaluate(poly, xValue).get(0);
      return Math.exp(resValue);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      ValueDerivatives resValue = FUNCTION.evaluateAndDifferentiate(poly, xValue);
      return Math.exp(resValue.getValue()) * resValue.getDerivative(0);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int nParams = yValues.length;
      double resValue = FUNCTION.evaluate(poly, xValue).get(0);
      DoubleArray resSense = FUNCTION.nodeSensitivity(polySens.get(), xValue);
      double expResValue = Math.exp(resValue);
      double[] res = new double[nParams];
      for (int i = 0; i < nParams; ++i) {
        res[i] = resSense.get(i + 1) * expResValue / yValues[i];
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
