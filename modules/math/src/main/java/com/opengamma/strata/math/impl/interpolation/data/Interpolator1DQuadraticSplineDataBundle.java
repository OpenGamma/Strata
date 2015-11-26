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
public class Interpolator1DQuadraticSplineDataBundle implements Interpolator1DDataBundle {

  private final Interpolator1DDataBundle _underlyingData;
  private final double[] _a;
  private final double[] _b;

  public Interpolator1DQuadraticSplineDataBundle(Interpolator1DDataBundle underlyingData) {
    ArgChecker.notNull(underlyingData, "underlying data");
    _underlyingData = underlyingData;
    double[] x = _underlyingData.getKeys();
    double[] h = _underlyingData.getValues();
    int n = _underlyingData.size();
    double[] dx = new double[n];
    dx[0] = x[0];
    for (int i = 1; i < n; i++) {
      dx[i] = x[i] - x[i - 1];
    }
    _a = new double[n + 1];
    _b = new double[n + 1];
    _a[0] = Math.sqrt(_underlyingData.firstValue() / _underlyingData.firstKey());

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
  public boolean containsKey(double key) {
    return _underlyingData.containsKey(key);
  }

  @Override
  public double firstKey() {
    return _underlyingData.firstKey();
  }

  @Override
  public double firstValue() {
    return _underlyingData.firstValue();
  }

  @Override
  public double get(double key) {
    return _underlyingData.get(key);
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(double key) {
    return _underlyingData.getBoundedValues(key);
  }

  @Override
  public double[] getKeys() {
    return _underlyingData.getKeys();
  }

  @Override
  public int getLowerBoundIndex(double value) {
    double[] keys = _underlyingData.getKeys();
    int n = _underlyingData.size();
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
  public double getLowerBoundKey(double value) {
    return _underlyingData.getLowerBoundKey(value);
  }

  @Override
  public double[] getValues() {
    return _underlyingData.getValues();
  }

  @Override
  public double higherKey(double key) {
    return _underlyingData.higherKey(key);
  }

  @Override
  public double higherValue(double key) {
    return _underlyingData.higherValue(key);
  }

  @Override
  public double lastKey() {
    return _underlyingData.lastKey();
  }

  @Override
  public double lastValue() {
    return _underlyingData.lastValue();
  }

  @Override
  public int size() {
    return _underlyingData.size();
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
