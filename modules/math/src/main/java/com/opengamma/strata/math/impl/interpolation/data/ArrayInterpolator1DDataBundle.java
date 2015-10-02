/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * An implementation of {@link Interpolator1DDataBundle} which holds all data in two
 * parallel-sorted double arrays.
 */
public class ArrayInterpolator1DDataBundle implements Interpolator1DDataBundle {
  private final double[] _keys;
  private final double[] _values;
  private final int _n;

  public ArrayInterpolator1DDataBundle(final double[] keys, final double[] values) {
    this(keys, values, false);
  }

  public ArrayInterpolator1DDataBundle(final double[] keys, final double[] values, final boolean inputsSorted) {
    ArgChecker.notNull(keys, "Keys must not be null.");
    ArgChecker.notNull(values, "Values must not be null.");
    ArgChecker.isTrue((keys.length == values.length), "keys and values must be same length.");
    ArgChecker.isTrue((keys.length > 0), "Must have at least two data points.");
    _keys = Arrays.copyOf(keys, keys.length);
    _values = Arrays.copyOf(values, values.length);
    _n = keys.length;
    if (!inputsSorted) {
      parallelBinarySort();
    }
    checkSameKeys();
  }

  private void checkSameKeys() {
    for (int i = 1; i < _n; i++) {
      ArgChecker.isTrue(Double.doubleToLongBits(_keys[i - 1]) != Double.doubleToLongBits(_keys[i]), "Equal nodes in interpolator {}", _keys[i - 1]);
    }
  }

  /**
   * Sort the content of _keys and _values simultaneously so that
   * both match the correct ordering.
   */
  private void parallelBinarySort() {
    dualArrayQuickSort(_keys, _values, 0, _n - 1);
  }

  private static void dualArrayQuickSort(final double[] keys, final double[] values, final int left, final int right) {
    if (right > left) {
      final int pivot = (left + right) >> 1;
      final int pivotNewIndex = partition(keys, values, left, right, pivot);
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1);
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right);
    }
  }

  private static int partition(final double[] keys, final double[] values, final int left, final int right,
      final int pivot) {
    final double pivotValue = keys[pivot];
    swap(keys, values, pivot, right);
    int storeIndex = left;
    for (int i = left; i < right; i++) {
      if (keys[i] <= pivotValue) {
        swap(keys, values, i, storeIndex);
        storeIndex++;
      }
    }
    swap(keys, values, storeIndex, right);
    return storeIndex;
  }

  private static void swap(final double[] keys, final double[] values, final int first, final int second) {
    double t = keys[first];
    keys[first] = keys[second];
    keys[second] = t;

    t = values[first];
    values[first] = values[second];
    values[second] = t;
  }

  @Override
  public boolean containsKey(final Double key) {
    if (key == null) {
      return false;
    }
    return Arrays.binarySearch(_keys, key) >= 0;
  }

  @Override
  public Double firstKey() {
    return _keys[0];
  }

  @Override
  public Double firstValue() {
    return _values[0];
  }

  @Override
  public Double get(final Double key) {
    final int index = Arrays.binarySearch(_keys, key);
    if (index < 0) {
      return null;
    }
    return _values[index];
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(final Double key) {
    final int index = getLowerBoundIndex(key);
    if (index == _n - 1) {
      return new InterpolationBoundedValues(index, _keys[index], _values[index], null, null);
    }
    return new InterpolationBoundedValues(index, _keys[index], _values[index], _keys[index + 1], _values[index + 1]);
  }

  @Override
  public double[] getKeys() {
    return _keys;
  }

  @Override
  public int getLowerBoundIndex(final Double value) {
    if (value < _keys[0]) {
      throw new IllegalArgumentException("Could not get lower bound index for " + value + ": lowest x-value is "
          + _keys[0]);
    }
    if (value > _keys[_n - 1]) {
      throw new IllegalArgumentException("Could not get lower bound index for " + value + ": highest x-value is "
          + _keys[_keys.length - 1]);
    }
    int index = Arrays.binarySearch(_keys, value);
    if (index >= 0) {
      // Fast break out if it's an exact match.
      return index;
    }
    if (index < 0) {
      index = -(index + 1);
      index--;
    }
    if (value == -0. && index < _n - 1 && _keys[index + 1] == 0.) {
      ++index;
    }
    return index;
  }

  @Override
  public Double getLowerBoundKey(final Double value) {
    final int index = getLowerBoundIndex(value);
    return _keys[index];
  }

  @Override
  public double[] getValues() {
    return _values;
  }

  @Override
  public Double higherKey(final Double key) {
    final int index = getHigherIndex(key);
    if (index >= _n) {
      return null;
    }
    return _keys[index];
  }

  @Override
  public Double higherValue(final Double key) {
    final int index = getHigherIndex(key);
    if (index >= _n) {
      return null;
    }
    return _values[index];
  }

  protected int getHigherIndex(final Double key) {
    return getLowerBoundIndex(key) + 1;
  }

  @Override
  public Double lastKey() {
    return _keys[_n - 1];
  }

  @Override
  public Double lastValue() {
    return _values[_n - 1];
  }

  @Override
  public int size() {
    return _keys.length;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_keys);
    result = prime * result + Arrays.hashCode(_values);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ArrayInterpolator1DDataBundle other = (ArrayInterpolator1DDataBundle) obj;
    if (!Arrays.equals(_keys, other._keys)) {
      return false;
    }
    if (!Arrays.equals(_values, other._values)) {
      return false;
    }
    return true;
  }

  @Override
  public void setYValueAtIndex(final int index, final double y) {
    ArgChecker.notNegative(index, "index");
    if (index >= _n) {
      throw new IllegalArgumentException("Index was greater than number of data points");
    }
    _values[index] = y;
  }
}
