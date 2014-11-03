package com.opengamma.platform.curves;

/**
 * Describes the methods available for extrapolation
 * of curve data.
 */
public enum ExtrapolationMethod {
  /**
   * Flat extrapolation: the extrapolant will have the same
   * value as the nearest pont in the data range.
   */
  FLAT,
  /**
   * Linear extrapolation: the extrapolant is based on the
   * gradient of the nearest point in the data range.
   */
  LINEAR,
  /**
   * Log-linear extrapolator: the extrapolant is exp(f(x)) where f(x) is a
   * linear function which is smoothly connected with a log-interpolator exp(F(x))
   * i.e., F'(x) = f'(x) at a respective endpoint.
   */
  LOG_LINEAR,
  /**
   * Extrapolator based on a exponential function. Outside the data range the
   * function is an exponential exp(m*x) where m is such that
   * <ul>
   *   <li>on the left: exp(m * data.firstKey()) = data.firstValue()</li>
   *   <li>on the right: exp(m * data.lastKey()) = data.lastValue()</li>
   *  </ul>
   */
  EXPONENTIAL
}
