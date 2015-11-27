/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform;

/**
 * This allows one to fit an interpolated curve, where the y-coordinates of the curve must lie in a certain range (e.g. nowhere must the
 * curve be below 0 or above 1 for any value of x), using any base interpolator and a function (and its inverse) that maps the constrained range
 * (e.g. 0 to 1 inclusive) to -infinity to +infinity. <p>
 * Let y(x) be an interpolated value and y_i the set of node values. An interpolated value can
 * be written as y(x) = I(x, (x_i,y_i)) - for a given interpolator, it is a function of x, and the node coordinates (x_i,y_i). However even
 * if all the node values (y_i) are constrained to the required range, there is no guarantee that y(x) will be in the range for all x (it depends
 * on the interpolator).<p>
 * Now let y*(x) and y*_i be the corresponding transformed values (i.e. the range is the entire real line), so y* = f(y).  We may wish to
 * work directly with the transformed values y*_i (since this allows unconstrained optimisation to find the values, where the interpolated curve
 * is part of some larger calibration). To this end we have y(x) = f^-1[I(x,(x_i,y*_i))], which is not strictly an interpolator since the curve
 * y(x) does not go through the points y*_i. We could of course write  y(x) = f^-1[I(x,(x_i,f(y_i)))], which is a true interpolator
 * (and could be write as I*(x,(x_i,y_i)) ). In both these cases y(x) is guaranteed to be in the range regardless of the base interpolator used.
 */
public class TransformedInterpolator1D extends Interpolator1D {

  private static final long serialVersionUID = 1L;
  private final ParameterLimitsTransform _transform;
  private final Interpolator1D _base;

  /**
   * 
   * @param baseInterpolator The interpolator used for interpolating in the transformed space
   * @param transform a two way mapping between a limited range and the real line
   */
  public TransformedInterpolator1D(final Interpolator1D baseInterpolator, final ParameterLimitsTransform transform) {
    ArgChecker.notNull(baseInterpolator, "null baseInterpolator");
    ArgChecker.notNull(transform, "null transform");
    _base = baseInterpolator;
    _transform = transform;
  }

  @Override
  public double interpolate(final Interpolator1DDataBundle data, final double value) {
    return _transform.inverseTransform(_base.interpolate(data, value));
  }

  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final double value) {
    return _transform.inverseTransformGradient(_base.interpolate(data, value)) * _base.firstDerivative(data, value);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final double value) {

    final double yStar = _base.interpolate(data, value);
    final double grad = _transform.inverseTransformGradient(yStar);
    final double[] temp = _base.getNodeSensitivitiesForValue(data, value);

    final int n = temp.length;
    for (int i = 0; i < n; i++) {
      temp[i] *= grad;
    }
    return temp;
  }

  /**
   * The node values must be in the transformed space
   * @param x The positions of the nodes (not necessarily in order)
   * @param y The values of the nodes - these must be in the transformed space
   * @return a data bundle
   */
  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return _base.getDataBundle(x, y);
  }

  /**
   * The node values must be in the transformed space
   * @param x The positions of the nodes. <b>These must be in ascending order</b>
   * @param y The values of the nodes - these must be in the transformed space
   * @return a data bundle
   */
  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return _base.getDataBundleFromSortedArrays(x, y);
  }

}
