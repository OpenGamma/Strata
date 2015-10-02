/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * If we sample an interpolated curve at a fix set of points (the sample points), then this can be viewed as a
 * vector to vector mapping. This provides that map and the associated Jacobian 
 */
public class InterpolatedCurveVectorFunction extends VectorFunction {
  private final double[] _samplePoints;
  private final Interpolator1D _interpolator;
  private final double[] _knots;

  /**
   * create an InterpolatedCurveVectorFunction
   * @param samplePoints position where the (interpolated) curve is sampled 
   * @param interpolator The interpolator 
   * @param knots knots of the interpolated curve - must be in ascending order 
   */
  public InterpolatedCurveVectorFunction(final double[] samplePoints, final Interpolator1D interpolator, final double[] knots) {

    ArgChecker.notEmpty(samplePoints, "samplePoints");
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notEmpty(knots, "knots");
    final int n = knots.length;
    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(knots[i] > knots[i - 1], "knot points must be strictly ascending");
    }
    _samplePoints = samplePoints.clone();
    _interpolator = interpolator;
    _knots = knots.clone();
  }

  @Override
  public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
    final Interpolator1DDataBundle db = _interpolator.getDataBundleFromSortedArrays(_knots, x.getData());
    final int n = _samplePoints.length;
    final int nKnots = _knots.length;
    final DoubleMatrix2D res = new DoubleMatrix2D(n, nKnots);
    final double[][] data = res.getData(); //direct access to matrix data
    for (int i = 0; i < n; i++) {
      data[i] = _interpolator.getNodeSensitivitiesForValue(db, _samplePoints[i]);
    }
    return res;
  }

  @Override
  public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
    final Interpolator1DDataBundle db = _interpolator.getDataBundleFromSortedArrays(_knots, x.getData());
    final int n = _samplePoints.length;
    final DoubleMatrix1D res = new DoubleMatrix1D(n);
    final double[] data = res.getData(); //direct access to vector data
    for (int i = 0; i < n; i++) {
      data[i] = _interpolator.interpolate(db, _samplePoints[i]);
    }
    return res;
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
