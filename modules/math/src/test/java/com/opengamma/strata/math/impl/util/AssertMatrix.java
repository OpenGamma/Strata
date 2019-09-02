/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Tests for whether vectors and matrices are equal within  some tolerance 
 */
public abstract class AssertMatrix {

  /**
  * Assert that two vectors (as {@link DoubleArray}) equal concerning a delta. To be equal the vectors
   * must be the same length, and each element must match to within delta. 
   * If they are not equal an AssertionFailedError is thrown.
   * @param v1 expected vector 
   * @param v2 actual vector 
   * @param delta the allowed difference between the elements 
   */
  public static void assertEqualsVectors(DoubleArray v1, DoubleArray v2, double delta) {
    ArgChecker.notNull(v1, "v1");
    ArgChecker.notNull(v2, "v2");
    int size = v1.size();
    assertThat(v2.size()).isEqualTo(size);

    for (int i = 0; i < size; i++) {
      assertThat(v1.get(i)).isCloseTo(v2.get(i), offset(delta));
    }
  }

  /**
   * Assert that two matrices (as {@link DoubleMatrix}) equal concerning a delta. To be equal the matrices
   * must be the same size, and each element must match to within delta. 
   * If they are not equal an AssertionFailedError is thrown.
   * @param m1 expected matrix
   * @param m2 actual matrix 
   * @param delta the allowed difference between the elements 
   */
  public static void assertEqualsMatrix(DoubleMatrix m1, DoubleMatrix m2, double delta) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    int rows = m1.rowCount();
    int cols = m1.columnCount();
    assertThat(rows).isEqualTo(m2.rowCount());
    assertThat(cols).isEqualTo(m2.columnCount());
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        assertThat(m1.get(i, j)).isCloseTo(m2.get(i, j), offset(delta));
      }
    }
  }

}
