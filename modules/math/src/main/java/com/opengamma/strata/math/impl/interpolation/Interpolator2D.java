/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Map;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A base class for two-dimensional interpolation.
 */
public abstract class Interpolator2D {

  /**
   * @param dataBundle
   *          A map of (x, y) pairs to z values.
   * @param value
   *          The (x, y) value for which an interpolated value for z is to be
   *          found.
   * @return The value of z
   */
  public abstract Double interpolate(Map<Double, Interpolator1DDataBundle> dataBundle, DoublesPair value);

  public abstract Map<DoublesPair, Double> getNodeSensitivitiesForValue(
      Map<Double, Interpolator1DDataBundle> dataBundle,
      DoublesPair value);

}
