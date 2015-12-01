/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Extrapolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * The standard implementation of a bound interpolator.
 */
final class ImmutableBoundCurveInterpolator
    implements BoundCurveInterpolator {

  /**
   * The underlying interpolator.
   */
  private final Interpolator1D interpolator;
  /**
   * The underlying extrapolator.
   */
  private final Extrapolator1D extrapolatorLeft;
  /**
   * The underlying extrapolator.
   */
  private final Extrapolator1D extrapolatorRight;
  /**
   * The underlying interpolator data bundle.
   */
  private final Interpolator1DDataBundle dataBundle;

  /**
   * Creates an instance.
   * 
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param interpolator  the underlying interpolator
   * @param extrapolatorLeft  the extrapolator for x-values on the left
   * @param extrapolatorRight  the extrapolator for x-values on the right
   */
  public ImmutableBoundCurveInterpolator(
      DoubleArray xValues,
      DoubleArray yValues,
      Interpolator1D interpolator,
      Extrapolator1D extrapolatorLeft,
      Extrapolator1D extrapolatorRight) {

    this.interpolator = ArgChecker.notNull(interpolator, "interpolator");
    this.extrapolatorLeft = ArgChecker.notNull(extrapolatorLeft, "extrapolatorLeft");
    this.extrapolatorRight = ArgChecker.notNull(extrapolatorRight, "extrapolatorRight");
    this.dataBundle = interpolator.getDataBundleFromSortedArrays(xValues.toArray(), yValues.toArray());
  }

  //-------------------------------------------------------------------------
  @Override
  public double yValue(double x) {
    if (x < dataBundle.firstKey()) {
      return extrapolatorLeft.extrapolate(dataBundle, x, interpolator);
    } else if (x > dataBundle.lastKey()) {
      return extrapolatorRight.extrapolate(dataBundle, x, interpolator);
    }
    return interpolator.interpolate(dataBundle, x);
  }

  @Override
  public DoubleArray yValueParameterSensitivity(double x) {
    return DoubleArray.ofUnsafe(yValueSensitivity(x));
  }

  private double[] yValueSensitivity(double x) {
    if (x < dataBundle.firstKey()) {
      return extrapolatorLeft.getNodeSensitivitiesForValue(dataBundle, x, interpolator);
    } else if (x > dataBundle.lastKey()) {
      return extrapolatorRight.getNodeSensitivitiesForValue(dataBundle, x, interpolator);
    }
    return interpolator.getNodeSensitivitiesForValue(dataBundle, x);
  }

  @Override
  public double firstDerivative(double x) {
    if (x < dataBundle.firstKey()) {
      return extrapolatorLeft.firstDerivative(dataBundle, x, interpolator);
    } else if (x > dataBundle.lastKey()) {
      return extrapolatorRight.firstDerivative(dataBundle, x, interpolator);
    }
    return interpolator.firstDerivative(dataBundle, x);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Interpolator[interpolator=");
    sb.append(interpolator.toString());
    sb.append(", extrapolatorLeft=");
    sb.append(extrapolatorLeft.toString());
    sb.append(", extrapolatorRight=");
    sb.append(extrapolatorRight.toString());
    sb.append("]");
    return sb.toString();
  }

}
