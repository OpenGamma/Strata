/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

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
    private final Interpolator1D interpolator;
    private final Interpolator1DDataBundle dataBundle;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      ArgChecker.isTrue(interpolator instanceof StandardBoundCurveInterpolator);
      StandardBoundCurveInterpolator mathInterpolator = ((StandardBoundCurveInterpolator) interpolator);
      this.dataBundle = mathInterpolator.getDataBundle();
      this.interpolator = mathInterpolator.getInterpolator();
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return interpolator.interpolate(dataBundle, xValue);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return interpolator.firstDerivative(dataBundle, xValue);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      return DoubleArray.ofUnsafe(interpolator.getNodeSensitivitiesForValue(dataBundle, xValue));
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return interpolator.interpolate(dataBundle, xValue);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return interpolator.firstDerivative(dataBundle, xValue);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      return DoubleArray.ofUnsafe(interpolator.getNodeSensitivitiesForValue(dataBundle, xValue));
    }
  }

}
