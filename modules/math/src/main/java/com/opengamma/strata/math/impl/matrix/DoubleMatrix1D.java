/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A minimal implementation of a vector (in the mathematical sense) that contains doubles.
 */
public class DoubleMatrix1D implements Matrix<Double> {
  private final double[] _data;
  private final int _elements;
  /** Empty vector */
  public static final DoubleMatrix1D EMPTY_MATRIX = new DoubleMatrix1D(new double[0]);

  /**
   * @param data The data, not null
   */
  public DoubleMatrix1D(final Double[] data) {
    ArgChecker.notNull(data, "data");
    _elements = data.length;
    _data = new double[_elements];
    for (int i = 0; i < _elements; i++) {
      _data[i] = data[i];
    }
  }

  /**
   * @param data The data, not null
   */
  public DoubleMatrix1D(final double... data) {
    ArgChecker.notNull(data, "data");
    _elements = data.length;
    _data = Arrays.copyOf(data, _elements);
  }

  /**
   * Create an vector of length n with all entries equal to value
   * @param n number of elements
   * @param value value of elements
   */
  public DoubleMatrix1D(final int n, final double value) {
    _elements = n;
    _data = new double[_elements];
    Arrays.fill(_data, value);
  }

  /**
   * Create an vector of length n with all entries zero
   * @param n number of elements
   */
  public DoubleMatrix1D(final int n) {
    _elements = n;
    _data = new double[_elements];
  }

  /**
   * Returns the underlying vector data. If this is changed so is the vector.
   * @see #toArray to get a copy of data
   * @return An array containing the vector elements
   */
  public double[] getData() {
    return _data;
  }

  /**
   * Convert the vector to a double array.
   * As its elements are copied, the array is independent from the vector data.
   * @return An array containing a copy of vector elements
   */
  public double[] toArray() {
    return Arrays.copyOf(_data, _elements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfElements() {
    return _elements;
  }

  /**
   * {@inheritDoc}
   * This method expects one index - any subsequent indices will be ignored.
   */
  @Override
  public Double getEntry(final int... index) {
    return _data[index[0]];
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_data);
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
    final DoubleMatrix1D other = (DoubleMatrix1D) obj;
    if (!Arrays.equals(_data, other._data)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    final int n = _data.length;
    sb.append(" (");
    for (int i = 0; i < (n - 1); i++) {
      sb.append(_data[i] + ", ");
    }
    sb.append(_data[n - 1] + ") ");
    return sb.toString();
  }
}
