/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * The standard implementation of a bound interpolator.
 */
final class StandardBoundCurveInterpolator
    implements BoundCurveInterpolator {

  /**
   * The underlying interpolator.
   */
  private final Interpolator1D interpolator;
  /**
   * The underlying extrapolator.
   */
  private final BoundCurveExtrapolator extrapolatorLeft;
  /**
   * The underlying extrapolator.
   */
  private final BoundCurveExtrapolator extrapolatorRight;
  /**
   * The underlying interpolator data bundle.
   */
  private final Interpolator1DDataBundle dataBundle;
  /**
   * The first x-value.
   */
  private final double firstXValue;
  /**
   * The last x-value.
   */
  private final double lastXValue;

  /**
   * Creates an instance that cannot perform extrapolation.
   * 
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   * @param interpolator  the underlying interpolator
   */
  StandardBoundCurveInterpolator(
      DoubleArray xValues,
      DoubleArray yValues,
      Interpolator1D interpolator) {

    this(xValues, yValues, interpolator, ExceptionCurveExtrapolator.INSTANCE, ExceptionCurveExtrapolator.INSTANCE);
  }

  /**
   * Creates an instance using the specified extrapolators.
   * 
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   * @param interpolator  the underlying interpolator
   * @param extrapolatorLeft  the bound extrapolator for x-values on the left
   * @param extrapolatorRight  the bound extrapolator for x-values on the right
   */
  StandardBoundCurveInterpolator(
      DoubleArray xValues,
      DoubleArray yValues,
      Interpolator1D interpolator,
      BoundCurveExtrapolator extrapolatorLeft,
      BoundCurveExtrapolator extrapolatorRight) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notNull(extrapolatorLeft, "extrapolatorLeft");
    ArgChecker.notNull(extrapolatorRight, "extrapolatorRight");
    ArgChecker.isTrue(xValues.size() == yValues.size(), "Curve node arrays must have same size");
    ArgChecker.isTrue(xValues.size() > 1, "Curve node arrays must have at least two nodes");
    this.interpolator = interpolator;
    this.extrapolatorLeft = extrapolatorLeft;
    this.extrapolatorRight = extrapolatorRight;
    this.dataBundle = interpolator.getDataBundleFromSortedArrays(xValues.toArray(), yValues.toArray());
    this.firstXValue = xValues.get(0);
    this.lastXValue = xValues.get(xValues.size() - 1);
  }

  // creates an instance when binding extrapolators, sharing common data
  private StandardBoundCurveInterpolator(
      StandardBoundCurveInterpolator base,
      BoundCurveExtrapolator extrapolatorLeft,
      BoundCurveExtrapolator extrapolatorRight) {
    
    ArgChecker.notNull(extrapolatorLeft, "extrapolatorLeft");
    ArgChecker.notNull(extrapolatorRight, "extrapolatorRight");
    this.interpolator = base.interpolator;
    this.extrapolatorLeft = extrapolatorLeft;
    this.extrapolatorRight = extrapolatorRight;
    this.dataBundle = base.dataBundle;
    this.firstXValue = base.firstXValue;
    this.lastXValue = base.lastXValue;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying interpolator.
   * 
   * @return the interpolator
   */
  Interpolator1D getInterpolator() {
    return interpolator;
  }

  /**
   * Gets the underlying data bundle.
   * 
   * @return the data bundle
   */
  Interpolator1DDataBundle getDataBundle() {
    return dataBundle;
  }

  //-------------------------------------------------------------------------
  @Override
  public double interpolate(double x) {
    if (x < firstXValue) {
      return extrapolatorLeft.leftExtrapolate(x);
    } else if (x > lastXValue) {
      return extrapolatorRight.rightExtrapolate(x);
    }
    return interpolator.interpolate(dataBundle, x);
  }

  @Override
  public double firstDerivative(double x) {
    if (x < firstXValue) {
      return extrapolatorLeft.leftExtrapolateFirstDerivative(x);
    } else if (x > lastXValue) {
      return extrapolatorRight.rightExtrapolateFirstDerivative(x);
    }
    return interpolator.firstDerivative(dataBundle, x);
  }

  @Override
  public DoubleArray parameterSensitivity(double x) {
    if (x < firstXValue) {
      return extrapolatorLeft.leftExtrapolateParameterSensitivity(x);
    } else if (x > lastXValue) {
      return extrapolatorRight.rightExtrapolateParameterSensitivity(x);
    }
    return DoubleArray.ofUnsafe(interpolator.getNodeSensitivitiesForValue(dataBundle, x));
  }

  @Override
  public BoundCurveInterpolator bind(
      BoundCurveExtrapolator extrapolatorLeft,
      BoundCurveExtrapolator extrapolatorRight) {

    return new StandardBoundCurveInterpolator(this, extrapolatorLeft, extrapolatorRight);
  }

}
