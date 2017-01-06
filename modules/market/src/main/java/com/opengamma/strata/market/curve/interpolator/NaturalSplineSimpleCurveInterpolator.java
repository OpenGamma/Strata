/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;

/**
 * Natural spline interpolator without sensitivity computed.
 * <p>
 * Use {@code CurveInterpolators.NATURAL_SPLINE} for parameter sensitivity.
 */
public class NaturalSplineSimpleCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "NaturalSplineSimple";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new NaturalSplineSimpleCurveInterpolator();

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The polynomial function.
   */
  private static final PiecewisePolynomialFunction1D FUNCTION = new PiecewisePolynomialFunction1D();

  /**
   * Restricted constructor.
   */
  private NaturalSplineSimpleCurveInterpolator() {
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

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      PiecewisePolynomialInterpolator underlying = new NaturalSplineInterpolator();
      this.poly = underlying.interpolate(xValues.toArray(), yValues.toArray());
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.poly = base.poly;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      return FUNCTION.evaluate(poly, xValue).get(0);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      return FUNCTION.differentiate(poly, xValue).get(0);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      throw new IllegalArgumentException("Use NaturalSplineCurveInterpolator for sensitivity");
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
