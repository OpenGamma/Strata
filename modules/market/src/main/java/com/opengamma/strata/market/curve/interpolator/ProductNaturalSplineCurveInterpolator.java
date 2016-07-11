/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * Product natural cubic spline interpolation.
 * <p>
 * Given a data set {@code (x[i], y[i])}, interpolate {@code (x[i], x[i] * y[i])} by natural cubic spline.
 * <p>
 * As a curve for the product {@code x * y} is not well-defined at {@code x = 0}, we impose
 * the condition that all of the x data to be the same sign, such that the origin is not within data range.
 * The x key value must not be close to zero.
 * <p>
 * See {@link NaturalSplineInterpolator} for the detail on the underlying interpolator.
 */
final class ProductNaturalSplineCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
  * The interpolator name.
  */
  public static final String NAME = "ProductNaturalSpline";
  /**
  * The interpolator instance.
  */
  public static final CurveInterpolator INSTANCE = new ProductNaturalSplineCurveInterpolator();

  /**
  * The serialization version id.
  */
  private static final long serialVersionUID = 1L;
  /**
   * The small parameter.
   */
  private static final double SMALL = 1e-10;
  /**
   * The polynomial function.
   */
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNCTION = new PiecewisePolynomialWithSensitivityFunction1D();

  /**
  * Restricted constructor.
  */
  private ProductNaturalSplineCurveInterpolator() {
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

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      ArgChecker.isTrue(xValues.get(0) > 0d || xValues.get(xValues.size() - 1) < 0d, "xValues must have the same sign");
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      double[] xyValues = getProduct(this.xValues, this.yValues);
      this.poly = new NaturalSplineInterpolator().interpolateWithSensitivity(xValues.toArray(), xyValues);
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.poly = base.poly;
    }

    //-------------------------------------------------------------------------
    private static double[] getProduct(double[] xValues, double[] yValues) {
      int nData = yValues.length;
      double[] xyValues = new double[nData];
      for (int i = 0; i < nData; ++i) {
        xyValues[i] = xValues[i] * yValues[i];
      }
      return xyValues;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      ArgChecker.isTrue(Math.abs(xValue) > SMALL, "magnitude of xValue must not be small");
      double resValue = FUNCTION.evaluate(poly, xValue).get(0);
      return resValue / xValue;
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      ArgChecker.isTrue(Math.abs(xValue) > SMALL, "magnitude of xValue must not be small");
      ValueDerivatives resValue = FUNCTION.evaluateAndDifferentiate(poly, xValue);
      return -resValue.getValue() / (xValue * xValue) + resValue.getDerivative(0) / xValue;
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      ArgChecker.isTrue(Math.abs(xValue) > SMALL, "magnitude of xValue must not be small");
      DoubleArray resSense = FUNCTION.nodeSensitivity(poly, xValue);
      return resSense.multipliedBy(DoubleArray.of(resSense.size(), i -> xValues[i] / xValue));
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
