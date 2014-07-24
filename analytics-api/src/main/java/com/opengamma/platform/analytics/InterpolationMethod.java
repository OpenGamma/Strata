package com.opengamma.platform.analytics;

import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;

/**
 * Describes the methods available for interpolation
 * of data.
 */
public enum InterpolationMethod {
  LINEAR(Interpolator1DFactory.LINEAR_INSTANCE),
  LOG_LINEAR(Interpolator1DFactory.LOG_LINEAR_INSTANCE),
  NATURAL_CUBIC_SPLINE(Interpolator1DFactory.NATURAL_CUBIC_SPLINE_INSTANCE),
  DOUBLE_QUADRATIC(Interpolator1DFactory.DOUBLE_QUADRATIC_INSTANCE);

  private final Interpolator1D interpolator;

  private InterpolationMethod(Interpolator1D interpolator) {
    this.interpolator = interpolator;
  }

  public Interpolator1D getInterpolator() {
    return interpolator;
  }
}
