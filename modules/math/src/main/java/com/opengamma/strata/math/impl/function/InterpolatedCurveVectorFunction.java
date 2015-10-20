/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * If we sample an interpolated curve at a fix set of points (the sample points),
 * then this can be viewed as a vector to vector mapping.
 * This provides that map and the associated Jacobian.
 */
public class InterpolatedCurveVectorFunction extends VectorFunction {

  private final double[] _samplePoints;
  private final Interpolator1D _interpolator;
  private final double[] _knots;

  /**
   * Creates an instance.
   * 
   * @param samplePoints  the position where the (interpolated) curve is sampled 
   * @param interpolator  the interpolator 
   * @param knots  the knots of the interpolated curve, must be in ascending order 
   */
  public InterpolatedCurveVectorFunction(double[] samplePoints, Interpolator1D interpolator, double[] knots) {
    ArgChecker.notEmpty(samplePoints, "samplePoints");
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notEmpty(knots, "knots");
    int n = knots.length;
    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(knots[i] > knots[i - 1], "knot points must be strictly ascending");
    }
    _samplePoints = samplePoints.clone();
    _interpolator = interpolator;
    _knots = knots.clone();
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix calculateJacobian(DoubleArray x) {
    Interpolator1DDataBundle db = _interpolator.getDataBundleFromSortedArrays(_knots, x.toArray());
    return DoubleMatrix.ofArrays(
        _samplePoints.length,
        _knots.length,
        i -> _interpolator.getNodeSensitivitiesForValue(db, _samplePoints[i]));
  }

  @Override
  public DoubleArray evaluate(DoubleArray x) {
    Interpolator1DDataBundle db = _interpolator.getDataBundleFromSortedArrays(_knots, x.toArray());
    return DoubleArray.of(_samplePoints.length, i -> _interpolator.interpolate(db, _samplePoints[i]));
  }

  @Override
  public int getLengthOfDomain() {
    return _knots.length;
  }

  @Override
  public int getLengthOfRange() {
    return _samplePoints.length;
  }

}
