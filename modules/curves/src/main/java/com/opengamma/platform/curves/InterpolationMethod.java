package com.opengamma.platform.curves;

import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;

/**
 * Describes the methods available for interpolation
 * of curve data. Each value is associated with an
 * interpolator instance which will perform the
 * interpolation when required.
 */
public enum InterpolationMethod {
  /**
   * A one-dimensional linear interpolator. The interpolated value of the function
   * <i>y</i> at <i>x</i> between two data points <i>(x<sub>1</sub>,
   * y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
   * <i>y = y<sub>1</sub> + (x - x<sub>1</sub>) * (y<sub>2</sub> - y<sub>1</sub>)
   * / (x<sub>2</sub> - x<sub>1</sub>)</i>
   */
  LINEAR(Interpolator1DFactory.LINEAR_INSTANCE),
  /**
   * A one-dimensional interpolator. The interpolated value of the function
   * <i>y</i> at <i>x</i> between two data points <i>(x<sub>1</sub>,
   * y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
   * <i>y = y<sub>1</sub> (y<sub>2</sub> / y<sub>1</sub>) ^ ((x - x<sub>1</sub>) /
   * (x<sub>2</sub> - x<sub>1</sub>))</i><br>
   * It is the equivalent of performing a linear interpolation on a data set after
   * taking the logarithm of the y-values.
   */
  LOG_LINEAR(Interpolator1DFactory.LOG_LINEAR_INSTANCE),
  /**
   * Cubic spline interpolation.
   */
  NATURAL_CUBIC_SPLINE(Interpolator1DFactory.NATURAL_CUBIC_SPLINE_INSTANCE),
  /**
   * Double quadratic interpolation.
   */
  DOUBLE_QUADRATIC(Interpolator1DFactory.DOUBLE_QUADRATIC_INSTANCE);

  /**
   * The interpolator to be used.
   */
  private final Interpolator1D interpolator;

  private InterpolationMethod(Interpolator1D interpolator) {
    this.interpolator = interpolator;
  }

  /**
   * Gets the associated interpolator instance.
   *
   * @return the interpolator
   */
  public Interpolator1D getInterpolator() {
    return interpolator;
  }
}
