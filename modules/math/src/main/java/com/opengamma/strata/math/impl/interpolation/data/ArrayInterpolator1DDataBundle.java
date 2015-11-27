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

  public ArrayInterpolator1DDataBundle(double[] keys, double[] values) {
    this(keys, values, false);
  }

  public ArrayInterpolator1DDataBundle(double[] keys, double[] values, boolean inputsSorted) {
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

  private static void dualArrayQuickSort(double[] keys, double[] values, int left, int right) {
    if (right > left) {
      int pivot = (left + right) >> 1;
      int pivotNewIndex = partition(keys, values, left, right, pivot);
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1);
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right);
    }
  }

  private static int partition(double[] keys, double[] values, int left, int right,
      int pivot) {
    double pivotValue = keys[pivot];
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

  private static void swap(double[] keys, double[] values, int first, int second) {
    double t = keys[first];
    keys[first] = keys[second];
    keys[second] = t;

    t = values[first];
    values[first] = values[second];
    values[second] = t;
  }

  @Override
  public boolean containsKey(double key) {
    return indexOf(key) >= 0;
  }

  @Override
  public double firstKey() {
    return _keys[0];
  }

  @Override
  public double firstValue() {
    return _values[0];
  }

  @Override
  public int indexOf(double key) {
    return Arrays.binarySearch(_keys, key);
  }

  @Override
  public double getIndex(int index) {
    return _values[index];
  }

  @Override
  public InterpolationBoundedValues getBoundedValues(double key) {
    int index = getLowerBoundIndex(key);
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
  public int getLowerBoundIndex(double value) {
    if (value < _keys[0]) {
      throw new IllegalArgumentException("Could not get lower bound index for " + value + ": lowest x-value is "
          + _keys[0]);
    }
    if (value > _keys[_n - 1]) {
      throw new IllegalArgumentException("Could not get lower bound index for " + value + ": highest x-value is "
          + _keys[_keys.length - 1]);
    }
    int index = indexOf(value);
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
  public double getLowerBoundKey(double value) {
    int index = getLowerBoundIndex(value);
    return _keys[index];
  }

  @Override
  public double getLowerBoundValue(double value) {
    int index = getLowerBoundIndex(value);
    return _values[index];
  }

  @Override
  public double[] getValues() {
    return _values;
  }

  @Override
  public double higherKey(double key) {
    int index = getHigherIndex(key);
    if (index >= _n) {
      throw new IllegalArgumentException("Key outside valid interpolation range: " + key);
    }
    return _keys[index];
  }

  @Override
  public double higherValue(double key) {
    int index = getHigherIndex(key);
    if (index >= _n) {
      throw new IllegalArgumentException("Key outside valid interpolation range: " + key);
    }
    return _values[index];
  }

  protected int getHigherIndex(double key) {
    return getLowerBoundIndex(key) + 1;
  }

  @Override
  public double lastKey() {
    return _keys[_n - 1];
  }

  @Override
  public double lastValue() {
    return _values[_n - 1];
  }

  @Override
  public int size() {
    return _keys.length;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_keys);
    result = prime * result + Arrays.hashCode(_values);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArrayInterpolator1DDataBundle other = (ArrayInterpolator1DDataBundle) obj;
    if (!Arrays.equals(_keys, other._keys)) {
      return false;
    }
    if (!Arrays.equals(_values, other._values)) {
      return false;
    }
    return true;
  }

  @Override
  public void setYValueAtIndex(int index, double y) {
    ArgChecker.notNegative(index, "index");
    if (index >= _n) {
      throw new IllegalArgumentException("Index was greater than number of data points");
    }
    _values[index] = y;
  }
}
