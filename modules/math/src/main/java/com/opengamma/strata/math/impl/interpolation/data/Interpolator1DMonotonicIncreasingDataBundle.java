/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;

/**
 * 
 */
public class Interpolator1DMonotonicIncreasingDataBundle implements Interpolator1DDataBundle {
  private final Interpolator1DDataBundle _underlyingData;
  private final double[] _a;
  private final double[] _b;

  public Interpolator1DMonotonicIncreasingDataBundle(final Interpolator1DDataBundle underlyingData) {
    ArgChecker.notNull(underlyingData, "underlying data");
    _underlyingData = underlyingData;
    final double[] x = _underlyingData.getKeys();
    final double[] h = _underlyingData.getValues();
    final int n = _underlyingData.size();

    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(h[i] >= h[i - 1], "Data not increasing");
    }

    final double[] dx = new double[n];
    dx[0] = x[0];
    for (int i = 1; i < n; i++) {
      dx[i] = x[i] - x[i - 1];
    }
    _a = new double[n + 1];
    _b = new double[n + 1];
    _a[0] = _underlyingData.firstValue() / _underlyingData.firstKey();

    for (int i = 1; i < n; i++) {
      _a[i] = _a[i - 1] * Math.exp(_b[i - 1] * dx[i - 1]);
      final double temp = ((h[i] - h[i - 1]) / _a[i] - dx[i]) * 2 / dx[i] / dx[i];
      if (temp == 0) {
        _b[i] = 0.0;
      } else {
        _b[i] = solveForB(h[i - 1] - h[i], _a[i], dx[i], Math.max(-10, Math.min(10, temp)));
      }
    }
    _a[n] = _a[n - 1] * Math.exp(_b[n - 1] * dx[n - 1]);
  }

  private double solveForB(final double c, final double a, final double dx, final double startB) {
    final double eps = 1e-12;
    double f = c + a / startB * (Math.exp(startB * dx) - 1);
    double b = startB;
    int count = 0;
    while (Math.abs(f) > eps) {
      final double expB = Math.exp(b * dx);
      final double df = a * (dx * expB / b - (expB - 1) / b / b);
      b = b - f / df;
      f = c + a / b * (Math.exp(b * dx) - 1);
      if (count > 50) {
        throw new MathException("fail to solve for b");
      }
      count++;
    }
    return b;
  }

  @Override
  public boolean containsKey(final Double key) {
    return _underlyingData.containsKey(key);
  }

  @Override
  public Double firstKey() {
    return _underlyingData.firstKey();
  }

  @Override
  public Double firstValue() {
    return _underlyingData.firstValue();
  }

  @Override
  public Double get(final Double key) {
    return _underlyingData.get(key);
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(final Double key) {
    return _underlyingData.getBoundedValues(key);
  }

  @Override
  public double[] getKeys() {
    return _underlyingData.getKeys();
  }

  @Override
  public int getLowerBoundIndex(final Double value) {
    final double[] keys = _underlyingData.getKeys();
    final int n = _underlyingData.size();
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
  public Double getLowerBoundKey(final Double value) {
    return _underlyingData.getLowerBoundKey(value);
  }

  @Override
  public double[] getValues() {
    return _underlyingData.getValues();
  }

  @Override
  public Double higherKey(final Double key) {
    return _underlyingData.higherKey(key);
  }

  @Override
  public Double higherValue(final Double key) {
    return _underlyingData.higherValue(key);
  }

  @Override
  public Double lastKey() {
    return _underlyingData.lastKey();
  }

  @Override
  public Double lastValue() {
    return _underlyingData.lastValue();
  }

  @Override
  public int size() {
    return _underlyingData.size();
  }

  @Override
  public void setYValueAtIndex(final int index, final double y) {
  }

  public double getA(final int index) {
    return _a[index];
  }

  public double getB(final int index) {
    return _b[index];
  }

}
