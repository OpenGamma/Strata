/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 *
 */
public class CombinedInterpolatorExtrapolator extends Interpolator1D {

  private static final long serialVersionUID = 1L;
  private final Interpolator1D _interpolator;
  private final Extrapolator1D _leftExtrapolator;
  private final Extrapolator1D _rightExtrapolator;

  public CombinedInterpolatorExtrapolator(Interpolator1D interpolator) {
    ArgChecker.notNull(interpolator, "interpolator");

    InterpolatorExtrapolator extrapolator = new InterpolatorExtrapolator();
    _interpolator = interpolator;
    _leftExtrapolator = extrapolator;
    _rightExtrapolator = extrapolator;
  }

  public CombinedInterpolatorExtrapolator(Interpolator1D interpolator, Extrapolator1D extrapolator) {
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notNull(extrapolator, "extrapolator");

    _interpolator = interpolator;
    _leftExtrapolator = extrapolator;
    _rightExtrapolator = extrapolator;
  }

  public CombinedInterpolatorExtrapolator(
      Interpolator1D interpolator,
      Extrapolator1D leftExtrapolator,
      Extrapolator1D rightExtrapolator) {

    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notNull(leftExtrapolator, "left extrapolator");
    ArgChecker.notNull(rightExtrapolator, "right extrapolator");

    _interpolator = interpolator;
    _leftExtrapolator = leftExtrapolator;
    _rightExtrapolator = rightExtrapolator;
  }

  /**
   * Returns a combined interpolator and extrapolator which uses the specified interpolator and extrapolators.
   *
   * @param interpolator  the interpolator
   * @param leftExtrapolator  the extrapolator used for points to the left of the leftmost point in the data set
   * @param rightExtrapolator  the extrapolator used for points to the right of the rightmost point in the data set
   * @return a combined interpolator and extrapolator which uses the specified interpolator and extrapolators
   */
  public static CombinedInterpolatorExtrapolator of(
      CurveInterpolator interpolator,
      CurveExtrapolator leftExtrapolator,
      CurveExtrapolator rightExtrapolator) {

    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notNull(leftExtrapolator, "left extrapolator");
    ArgChecker.notNull(rightExtrapolator, "right extrapolator");

    if (!(interpolator instanceof Interpolator1D)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Interpolator {} is not an instance of Interpolator1D",
              interpolator));
    }
    if (!(leftExtrapolator instanceof Extrapolator1D)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Extrapolator {} is not an instance of Extrapolator1D",
              leftExtrapolator));
    }
    if (!(rightExtrapolator instanceof Extrapolator1D)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Extrapolator {} is not an instance of Extrapolator1D",
              rightExtrapolator));
    }
    return new CombinedInterpolatorExtrapolator(
        (Interpolator1D) interpolator,
        (Extrapolator1D) leftExtrapolator,
        (Extrapolator1D) rightExtrapolator);
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(double[] x, double[] y) {
    return _interpolator.getDataBundle(x, y);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(double[] x, double[] y) {
    return _interpolator.getDataBundleFromSortedArrays(x, y);
  }

  /**
   * Gets the underlying interpolator.
   *
   * @return the interpolator
   */
  public Interpolator1D getInterpolator() {
    return _interpolator;
  }

  /**
   * Gets the underlying left extrapolator.
   *
   * @return the left extrapolator
   */
  public Extrapolator1D getLeftExtrapolator() {
    return _leftExtrapolator;
  }

  /**
   * Gets the underlying right extrapolator.
   *
   * @return the right extrapolator
   */
  public Extrapolator1D getRightExtrapolator() {
    return _rightExtrapolator;
  }

  //TODO  fail earlier if there's no extrapolators?
  @Override
  public Double interpolate(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(data, "data");
    ArgChecker.notNull(value, "value");

    if (value < data.firstKey()) {
      return _leftExtrapolator.extrapolate(data, value, _interpolator);
    } else if (value > data.lastKey()) {
      return _rightExtrapolator.extrapolate(data, value, _interpolator);
    }
    return _interpolator.interpolate(data, value);
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(data, "data");
    ArgChecker.notNull(value, "value");

    if (value < data.firstKey()) {
      return _leftExtrapolator.firstDerivative(data, value, _interpolator);
    } else if (value > data.lastKey()) {
      return _rightExtrapolator.firstDerivative(data, value, _interpolator);
    }
    return _interpolator.firstDerivative(data, value);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(data, "data");
    ArgChecker.notNull(value, "value");

    if (value < data.firstKey()) {
      return _leftExtrapolator.getNodeSensitivitiesForValue(data, value, _interpolator);
    } else if (value > data.lastKey()) {
      return _rightExtrapolator.getNodeSensitivitiesForValue(data, value, _interpolator);
    }
    return _interpolator.getNodeSensitivitiesForValue(data, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Interpolator[interpolator=");
    sb.append(_interpolator.toString());
    sb.append(", left extrapolator=");
    sb.append(_leftExtrapolator.toString());
    sb.append(", right extrapolator=");
    sb.append(_rightExtrapolator.toString());
    sb.append("]");
    return sb.toString();
  }
}
