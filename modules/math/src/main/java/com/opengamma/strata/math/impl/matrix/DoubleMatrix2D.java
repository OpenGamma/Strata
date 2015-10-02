/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A minimal implementation of a 2D matrix of doubles.
 *
 */
public class DoubleMatrix2D implements Matrix<Double> {
  private final double[][] _data;
  private final int _rows;
  private final int _columns;
  private final int _elements;
  /** * Empty 2D matrix */
  public static final DoubleMatrix2D EMPTY_MATRIX = new DoubleMatrix2D(new double[0][0]);

  /**
   * Sets up an empty matrix
   * @param rows Number of rows
   * @param columns Number of columns
   */
  public DoubleMatrix2D(final int rows, final int columns) {
    ArgChecker.isTrue(rows > 0, "row number cannot be negative or zero");
    ArgChecker.isTrue(columns > 0, "column number cannot be negative or zero");
    _rows = rows;
    _columns = columns;
    _data = new double[_rows][_columns];
    _elements = _rows * _columns;
  }

  // REVIEW could do with a constructor that does NOT copy the data
  /**
   * @param data The data, not null. The data is expected in row-column form.
   * @throws IllegalArgumentException If the matrix is not rectangular
   */
  public DoubleMatrix2D(final double[][] data) {
    ArgChecker.notNull(data, "data");
    if (data.length == 0) {
      _data = new double[0][0];
      _elements = 0;
      _rows = 0;
      _columns = 0;
    } else {
      _rows = data.length;
      _columns = data[0].length;
      _data = new double[_rows][_columns];
      for (int i = 0; i < _rows; i++) {
        System.arraycopy(data[i], 0, _data[i], 0, data[i].length);
      }
      _elements = _rows * _columns;
    }
  }

  /**
   * @param data The data, not null. The data is expected in row-column form.
   * @throws IllegalArgumentException If the matrix is not rectangular
   */
  public DoubleMatrix2D(final Double[][] data) {
    ArgChecker.notNull(data, "data");
    if (data.length == 0) {
      _data = new double[0][0];
      _elements = 0;
      _rows = 0;
      _columns = 0;
    } else {
      _rows = data.length;
      _columns = data[0].length;
      _data = new double[_rows][_columns];
      for (int i = 0; i < _rows; i++) {
        for (int j = 0; j < _columns; j++) {
          _data[i][j] = data[i][j];
        }
      }
      _elements = _rows * _columns;
    }
  }

  /**
   * Returns the row for a particular index.
   * @param index The index
   * @return The row
   */
  public DoubleMatrix1D getRowVector(final int index) {
    return new DoubleMatrix1D(_data[index]);
  }

  /**
   * Returns the column for a particular index.
   * @param index The index
   * @return The column
   */
  public DoubleMatrix1D getColumnVector(final int index) {
    final double[] res = new double[_rows];
    for (int i = 0; i < _rows; i++) {
      res[i] = _data[i][index];
    }
    return new DoubleMatrix1D(res);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getEntry(final int... index) {
    ArgChecker.notNull(index, "indices");
    ArgChecker.isTrue(index[0] < _data.length, "x index {} is greater than length of array {}", index[0], _data.length);
    ArgChecker.isTrue(index[1] < _data[0].length, "y index {} is greater than length of array {}", index[1], _data[0].length);
    return _data[index[0]][index[1]];
  }

  /**
   * Returns the underlying matrix data. If this is changed so is the matrix.
   * @see #toArray to get a copy of data
   * @return An array of arrays containing the matrix elements
   */
  public double[][] getData() {
    return _data;
  }

  /**
   * Convert the matrix to an array of double arrays.
   * As its elements are copied, the array is independent from the matrix data.
   * @return An array of arrays containing a copy of matrix elements
   */
  public double[][] toArray() {
    final DoubleMatrix2D temp = new DoubleMatrix2D(_data);
    return temp.getData();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfElements() {
    return _elements;
  }

  /**
   * @return The number of rows in this matrix
   */
  public int getNumberOfRows() {
    return _rows;
  }

  /**
   * @return The number of columns in this matrix
   */
  public int getNumberOfColumns() {
    return _columns;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _columns;
    result = prime * result + _rows;
    int count = 0;
    for (int i = 0; i < _rows; i++) {
      for (int j = 0; j < _columns; j++) {
        result = prime * result + Double.valueOf(_data[i][j]).hashCode();
        if (count == 10) {
          break;
        }
        count++;
      }
      if (count == 10) {
        break;
      }
    }
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
    final DoubleMatrix2D other = (DoubleMatrix2D) obj;
    if (_columns != other._columns) {
      return false;
    }
    if (_rows != other._rows) {
      return false;
    }
    for (int i = 0; i < _rows; i++) {
      for (int j = 0; j < _columns; j++) {
        if (Double.doubleToLongBits(_data[i][j]) != Double.doubleToLongBits(other._data[i][j])) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    for (final double[] d : _data) {
      for (int i = 0; i < d.length; i++) {
        sb.append(d[i]);
        sb.append(i == d.length - 1 ? "\n" : "\t");
      }
    }
    return sb.toString();
  }
}
