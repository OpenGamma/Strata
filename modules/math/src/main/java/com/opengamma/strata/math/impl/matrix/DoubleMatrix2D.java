/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.function.IntIntDoubleConsumer;
import com.opengamma.strata.collect.function.IntIntDoubleToDoubleFunction;
import com.opengamma.strata.collect.function.IntIntToDoubleFunction;

/**
 * An immutable two-dimensional array of {@code double} values.
 * <p>
 * This provides functionality similar to {@link List} but for a rectangular {@code double[][]}.
 * <p>
 * In mathematical terms, this is a two-dimensional matrix.
 */
public class DoubleMatrix2D implements Matrix, Serializable {

  /**
   * An empty array.
   */
  public static final DoubleMatrix2D EMPTY = new DoubleMatrix2D(new double[0][0], 0, 0);

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The underlying array of doubles.
   */
  private final double[][] array;
  /**
   * The number of rows.
   */
  private final int rows;
  /**
   * The number of columns.
   */
  private final int columns;
  /**
   * The number of elements.
   */
  private final int elements;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance.
   * 
   * @return the empty immutable matrix
   */
  public static DoubleMatrix2D of() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with entries filled using a function.
   * <p>
   * The function is passed the row and column index, returning the value.
   * 
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param valueFunction  the function used to populate the value
   * @return a matrix initialized using the function
   */
  public static DoubleMatrix2D of(int rows, int columns, IntIntToDoubleFunction valueFunction) {
    if (rows == 0 || columns == 0) {
      return EMPTY;
    }
    double[][] array = new double[rows][columns];
    for (int i = 0; i < array.length; i++) {
      double[] inner = array[i];
      for (int j = 0; j < inner.length; j++) {
        array[i][j] = valueFunction.applyAsDouble(i, j);
      }
    }
    return new DoubleMatrix2D(array, rows, columns);
  }

  /**
   * Obtains an instance by wrapping a {@code double[][]}.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   * <p>
   * The {@code double[][]} must be rectangular, with the same length for each row.
   * This is not validated.
   * 
   * @param array  the array to assign
   * @return a matrix wrapping the specified array
   */
  public static DoubleMatrix2D ofUnsafe(double[][] array) {
    int rows = array.length;
    if (rows == 0 || array[0].length == 0) {
      return EMPTY;
    }
    return new DoubleMatrix2D(array, rows, array[0].length);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a {@code double[][]}.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy, cloned
   * @return a matrix containing the specified values
   */
  public static DoubleMatrix2D copyOf(double[][] array) {
    int rows = array.length;
    if (rows == 0 || array[0].length == 0) {
      return EMPTY;
    }
    // TODO: deep clone
    return new DoubleMatrix2D(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   * 
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @return a matrix filled with zeroes
   */
  public static DoubleMatrix2D filled(int rows, int columns) {
    if (rows == 0 || columns == 0) {
      return EMPTY;
    }
    return new DoubleMatrix2D(new double[rows][columns], rows, columns);
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   * 
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param value  the value of all the elements
   * @return a matrix filled with the specified value
   */
  public static DoubleMatrix2D filled(int rows, int columns, double value) {
    if (rows == 0 || columns == 0) {
      return EMPTY;
    }
    double[][] array = new double[rows][columns];
    for (int i = 0; i < array.length; i++) {
      Arrays.fill(array[i], value);
    }
    return new DoubleMatrix2D(array, rows, columns);
  }

  //-------------------------------------------------------------------------
  /**
   * Sets up an empty matrix.
   * 
   * @param data  the data
   * @param rows  the number of rows
   * @param columns  the number of columns
   */
  DoubleMatrix2D(double[][] data, int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
    this.array = data;
    this.elements = rows * columns;
  }

  //-------------------------------------------------------------------------
  // start of old code to be removed
  /**
   * @param data The data, not null. The data is expected in row-column form.
   * @throws IllegalArgumentException If the matrix is not rectangular
   */
  public DoubleMatrix2D(double[][] data) {
    ArgChecker.notNull(data, "data");
    if (data.length == 0) {
      this.array = new double[0][0];
      this.elements = 0;
      this.rows = 0;
      this.columns = 0;
    } else {
      this.rows = data.length;
      this.columns = data[0].length;
      this.array = new double[rows][columns];
      for (int i = 0; i < rows; i++) {
        System.arraycopy(data[i], 0, array[i], 0, data[i].length);
      }
      this.elements = rows * columns;
    }
  }

  /**
   * Returns the underlying matrix data. If this is changed so is the matrix.
   * @see #toArray to get a copy of data
   * @return An array of arrays containing the matrix elements
   */
  public double[][] getData() {
    return array;
  }

  // end of old code to be removed
  //-------------------------------------------------------------------------
  /**
   * Gets the number of dimensions of this matrix.
   * 
   * @return two
   */
  @Override
  public int dimensions() {
    return 2;
  }

  /**
   * Gets the size of this matrix.
   * <p>
   * This is the total number of elements.
   * 
   * @return the matrix size, zero or greater
   */
  @Override
  public int size() {
    return elements;
  }

  /**
   * Gets the number of rows of this matrix.
   * 
   * @return the number of rows
   */
  public int rowCount() {
    return rows;
  }

  /**
   * Gets the number of columns of this matrix.
   * 
   * @return the number of columns
   */
  public int columnCount() {
    return columns;
  }

  /**
   * Checks if this matrix is empty.
   * 
   * @return true if empty
   */
  public boolean isEmpty() {
    return elements == 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value at the specified row and column in this matrix.
   * 
   * @param row  the zero-based row index to retrieve
   * @param column  the zero-based column index to retrieve
   * @return the value at the row and column
   * @throws IndexOutOfBoundsException if either index is invalid
   */
  public double get(int row, int column) {
    return array[row][column];
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the row at the specified index.
   * 
   * @param row  the zero-based row index to retrieve
   * @return the row
   */
  public DoubleMatrix1D row(int row) {
    return DoubleMatrix1D.ofUnsafe(array[row]);
  }

  /**
   * Gets the column at the specified index.
   * 
   * @param column  the zero-based column index to retrieve
   * @return the column
   */
  public DoubleMatrix1D column(int column) {
    return DoubleMatrix1D.of(rows, i -> array[i][column]);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent {@code double[][]}.
   * 
   * @return an array of arrays containing a copy of matrix elements
   */
  public double[][] toArray() {
    DoubleMatrix2D temp = new DoubleMatrix2D(array);
    return temp.getData();
  }

  /**
   * Returns the underlying array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the array returned by this method.
   * Doing so would violate the immutability of this class.
   * 
   * @return the raw array
   */
  public double[][] toArrayUnsafe() {
    return array;
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each value in the matrix.
   * <p>
   * This is used to perform an action on the contents of this matrix.
   * The action receives the row, the column and the value.
   * For example, the action could print out the matrix.
   * <pre>
   *   base.forEach((row, col, value) -> System.out.println(row + ": " + col + ": " + value));
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param action  the action to be applied
   */
  public void forEach(IntIntDoubleConsumer action) {
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        action.accept(i, j, array[i][j]);
      }
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an instance with the value at the specified index changed.
   * <p>
   * This instance is immutable and unaffected by this method. 
   * 
   * @param row  the zero-based row index to retrieve
   * @param column  the zero-based column index to retrieve
   * @param newValue  the new value to store
   * @return a copy of this matrix with the value at the index changed
   * @throws IndexOutOfBoundsException if either index is invalid
   */
  public DoubleMatrix2D with(int row, int column, double newValue) {
    if (Double.doubleToLongBits(array[row][column]) == Double.doubleToLongBits(newValue)) {
      return this;
    }
    double[][] result = array.clone();  // shallow clone rows array
    result[row] = result[row].clone();  // clone the column actually being changed
    result[row][column] = newValue;
    return new DoubleMatrix2D(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with each value multiplied by the specified factor.
   * <p>
   * This is used to multiply the contents of this matrix, returning a new matrix.
   * <p>
   * This is a special case of {@link #map(DoubleUnaryOperator)}.
   * This instance is immutable and unaffected by this method. 
   * 
   * @param factor  the multiplicative factor
   * @return a copy of this matrix with the each value multiplied by the factor
   */
  public DoubleMatrix2D multipliedBy(double factor) {
    if (factor == 1d) {
      return this;
    }
    double[][] result = array.clone();
    for (int i = 0; i < rows; i++) {
      result[i] = result[i].clone();
      for (int j = 0; j < columns; j++) {
        result[i][j] = array[i][j] * factor;
      }
    }
    return new DoubleMatrix2D(result);
  }

  /**
   * Returns an instance with an operation applied to each value in the matrix.
   * <p>
   * This is used to perform an operation on the contents of this matrix, returning a new matrix.
   * The operator only receives the value.
   * For example, the operator could take the inverse of each element.
   * <pre>
   *   result = base.map(value -> 1 / value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param operator  the operator to be applied
   * @return a copy of this matrix with the operator applied to the original values
   */
  public DoubleMatrix2D map(DoubleUnaryOperator operator) {
    double[][] result = array.clone();
    for (int i = 0; i < rows; i++) {
      result[i] = result[i].clone();
      for (int j = 0; j < columns; j++) {
        result[i][j] = operator.applyAsDouble(array[i][j]);
      }
    }
    return new DoubleMatrix2D(result);
  }

  /**
   * Returns an instance with an operation applied to each indexed value in the matrix.
   * <p>
   * This is used to perform an operation on the contents of this matrix, returning a new matrix.
   * The function receives the row index, column index and the value.
   * For example, the operator could multiply the value by the index.
   * <pre>
   *   result = base.mapWithIndex((index, value) -> index * value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param function  the function to be applied
   * @return a copy of this matrix with the operator applied to the original values
   */
  public DoubleMatrix2D mapWithIndex(IntIntDoubleToDoubleFunction function) {
    double[][] result = array.clone();
    for (int i = 0; i < rows; i++) {
      result[i] = result[i].clone();
      for (int j = 0; j < columns; j++) {
        result[i][j] = function.applyAsDouble(i, j, array[i][j]);
      }
    }
    return new DoubleMatrix2D(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of all the values in the matrix.
   * <p>
   * This is a special case of {@link #reduce(double, DoubleBinaryOperator)}.
   * 
   * @return the total of all the values
   */
  public double total() {
    double total = 0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        total += array[i][j];
      }
    }
    return total;
  }

  /**
   * Reduces this matrix returning a single value.
   * <p>
   * This is used to reduce the values in this matrix to a single value.
   * The operator is called once for each element in the matrix.
   * The first argument to the operator is the running total of the reduction, starting from zero.
   * The second argument to the operator is the element.
   * <p>
   * This instance is immutable and unaffected by this method. 
   * 
   * @param identity  the identity value to start from
   * @param operator  the operator used to combine the value with the current total
   * @return the result of the reduction
   */
  public double reduce(double identity, DoubleBinaryOperator operator) {
    double result = identity;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        result = operator.applyAsDouble(result, array[i][j]);
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof DoubleMatrix2D) {
      DoubleMatrix2D other = (DoubleMatrix2D) obj;
      if (columns != other.columns || rows != other.rows) {
        return false;
      }
      for (int i = 0; i < rows; i++) {
        for (int j = 0; j < columns; j++) {
          if (Double.doubleToLongBits(array[i][j]) != Double.doubleToLongBits(other.array[i][j])) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < rows; i++) {
      result = 31 * result + Arrays.hashCode(array[i]);
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (double[] d : array) {
      for (int i = 0; i < d.length; i++) {
        buf.append(d[i]);
        buf.append(i == d.length - 1 ? "\n" : " ");
      }
    }
    return buf.toString();
  }

}
