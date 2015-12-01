/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Interface for extrapolators which can extrapolate beyond the ends of a set of data.
 */
public interface Extrapolator1D {

  /**
   * Returns an extrapolated output value for the specified input value, interpolator and data bundle.
   *
   * @param data  the data bundle associated with the interpolator
   * @param value  the input data point
   * @param interpolator  the interpolator used in conjunction with this extrapolator
   * @return an extrapolated output value for the specified input value, interpolator and data bundle
   */
  public abstract double extrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator);

  /**
   * Returns the first derivative of the data at the specified point.
   *
   * @param data  the data bundle associated with the interpolator
   * @param value  the input data point
   * @param interpolator  the interpolator used in conjunction with this extrapolator
   * @return the first derivative of the data at the specified point
   */
  public abstract double firstDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator);

  /**
   * Returns the node sensitivities of the data at the specified point.
   *
   * @param data  the data bundle associated with the interpolator
   * @param value  the input data point
   * @param interpolator  the interpolator used in conjunction with this extrapolator
   * @return the node sensitivities of the data at the specified point
   */
  public abstract double[] getNodeSensitivitiesForValue(
      Interpolator1DDataBundle data,
      double value,
      Interpolator1D interpolator);
}
