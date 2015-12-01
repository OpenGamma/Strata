/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class Interpolator1DQuadraticSplineDataBundle
    extends ForwardingInterpolator1DDataBundle {

  private final double[] _a;
  private final double[] _b;

  public Interpolator1DQuadraticSplineDataBundle(Interpolator1DDataBundle underlyingData) {
    super(underlyingData);
    double[] x = underlyingData.getKeys();
    double[] h = underlyingData.getValues();
    int n = underlyingData.size();
    double[] dx = new double[n];
    dx[0] = x[0];
    for (int i = 1; i < n; i++) {
      dx[i] = x[i] - x[i - 1];
    }
    _a = new double[n + 1];
    _b = new double[n + 1];
    _a[0] = Math.sqrt(underlyingData.firstValue() / underlyingData.firstKey());

    for (int i = 1; i < n; i++) {
      _a[i] = _a[i - 1] + _b[i - 1] * dx[i - 1];

      double a = Math.pow(dx[i], 3) / 3;
      double b = _a[i] * dx[i] * dx[i];
      double c = _a[i] * _a[i] * dx[i] + h[i - 1] - h[i];
      double root = b * b - 4 * a * c;
      ArgChecker.isTrue(root >= 0, "root of neg");
      root = Math.sqrt(root);
      double temp1 = (-b + root) / 2 / a;
      _b[i] = temp1;
    }
    _a[n] = _a[n - 1];
  }

  @Override
  public int getLowerBoundIndex(double value) {
    double[] keys = getUnderlying().getKeys();
    int n = getUnderlying().size();
    if (value < keys[0]) {
      return 0;
    }
    if (value > keys[n - 1]) {
      return n;
    }
    int index = Arrays.binarySearch(keys, value);
    if (index >= 0) {
      // Fast break out if it's an exact match.
      return index;
    }
    if (index < 0) {
      index = -(index + 1);
      index--;
    }
    return index;
  }

  @Override
  public void setYValueAtIndex(int index, double y) {
  }

  public double getA(int index) {
    return _a[index];
  }

  public double getB(int index) {
    return _b[index];
  }

}
