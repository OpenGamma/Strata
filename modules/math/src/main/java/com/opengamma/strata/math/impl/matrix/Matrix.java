/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

/**
 * Interface representing a matrix that can have an arbitrary number of dimensions and that contains an arbitrary type of data. 
 * @param <T> Type of elements
 */
public interface Matrix<T> {

  /**
   * @return The number of elements in this matrix
   */
  int getNumberOfElements();

  /**
   * Gets the entry specified by the indices. For example, for a 3-D matrix, the indices matrix must have three elements.
   * @param indices The indices, not null. The number of indices must match the dimension of the matrix
   * @return The entry 
   */
  T getEntry(int... indices);
}
