/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import com.opengamma.strata.collect.ArgChecker;

/**
 * An immutable identity matrix of {@code double} values.
 * <p>
 * An identity matrix is square. It has every value equal to zero, except those
 * on the primary diagonal, which asre one.
 */
public class IdentityMatrix extends DoubleMatrix {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;

  public IdentityMatrix(int size) {
    super(build(size), size, size);
  }

  private static double[][] build(int size) {
    ArgChecker.isTrue(size > 0, "size must be > 0");
    double[][] array = new double[size][size];
    for (int i = 0; i < size; i++) {
      array[i][i] = 1.0;
    }
    return array;
  }

  @Override
  public double get(int index1, int index2) {
    return index1 == index2 ? 1.0 : 0.0;
  }

  /**
   * The size (number of rows or columns) of the matrix
   * @return size
   */
  public int getSize() {
    return rowCount();
  }

}
