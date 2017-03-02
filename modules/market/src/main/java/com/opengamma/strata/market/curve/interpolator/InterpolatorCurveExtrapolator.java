/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation.
 * <p>
 * Extrapolator that does no extrapolation itself and delegates to the interpolator for all operations.
 * <p>
 * This extrapolator is used in place of a null extrapolator which allows the extrapolators to be non-null
 * and makes for simpler and cleaner code where the extrapolators are used.
 */
final class InterpolatorCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The interpolator name.
   */
  public static final String NAME = "Interpolator";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new InterpolatorCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private InterpolatorCurveExtrapolator() {
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
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    return new Bound(xValues, yValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound extrapolator.
   */
  static class Bound implements BoundCurveExtrapolator {
    private final AbstractBoundCurveInterpolator interpolator;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      ArgChecker.isTrue(interpolator instanceof AbstractBoundCurveInterpolator);
      this.interpolator = (AbstractBoundCurveInterpolator) interpolator;
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return interpolator.doInterpolate(xValue);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return interpolator.doFirstDerivative(xValue);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      return interpolator.doParameterSensitivity(xValue);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return interpolator.doInterpolate(xValue);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return interpolator.doFirstDerivative(xValue);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      return interpolator.doParameterSensitivity(xValue);
    }
  }

}
