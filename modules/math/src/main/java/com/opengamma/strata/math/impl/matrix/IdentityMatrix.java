/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class IdentityMatrix extends DoubleMatrix2D {

  private final int _size;

  public IdentityMatrix(final int size) {
    super(size, size);
    ArgChecker.isTrue(size > 0, "size must be > 0");
    for (int i = 0; i < size; i++) {
      getData()[i][i] = 1.0;
    }
    _size = size;
  }

  @Override
  public int getNumberOfElements() {
    return _size * _size;
  }

  @Override
  public Double getEntry(final int... indices) {
    ArgChecker.isTrue(indices.length == 2, "pass only two indices");
    return indices[0] == indices[1] ? 1.0 : 0.0;
  }

  /**
   * The size (number of rows or columns) of the matrix
   * @return size
   */
  public int getSize() {
    return _size;
  }

}
