/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Test.
 */
@SuppressWarnings("synthetic-access")
@Test
public class MatrixAlgebraTest {
  private static final MatrixAlgebra ALGEBRA = new MyMatrixAlgebra();

  private static final DoubleArray M1 = DoubleArray.of(1, 2);
  private static final DoubleArray M2 = DoubleArray.of(3, 4);
  private static final DoubleMatrix M3 = DoubleMatrix.of(2, 2, 1d, 2d, 3d, 4d);
  private static final DoubleMatrix M4 = DoubleMatrix.of(2, 2, 5d, 6d, 7d, 8d);
  private static final Matrix M5 = new Matrix() {
    @Override
    public int dimensions() {
      return 1;
    }

    @Override
    public int size() {
      return 0;
    }

  };
  private static final double EPS = 1e-10;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddWrongSize() {
    ALGEBRA.add(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddDifferentRowNumber1D() {
    ALGEBRA.add(M1, DoubleArray.of(1, 2, 3));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddDifferentRowNumber2D() {
    ALGEBRA.add(M3, DoubleMatrix.of(3, 2, 1d, 2d, 3d, 4d, 5d, 6d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddDifferentColumnNumber2D() {
    ALGEBRA.add(M3, DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddWrongType1() {
    ALGEBRA.add(M1, M5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddWrongType2() {
    ALGEBRA.add(M3, M5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testAddWrongType3() {
    ALGEBRA.add(M5, M5);
  }

  @Test
  public void testAdd() {
    Matrix m = ALGEBRA.add(M1, M2);
    assertTrue(m instanceof DoubleArray);
    assertMatrixEquals(m, DoubleArray.of(4, 6));
    m = ALGEBRA.add(M3, M4);
    assertTrue(m instanceof DoubleMatrix);
    assertMatrixEquals(m, DoubleMatrix.of(2, 2, 6d, 8d, 10d, 12d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractWrongSize() {
    ALGEBRA.subtract(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentRowNumber1D() {
    ALGEBRA.subtract(M1, DoubleArray.of(1, 2, 3));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentRowNumber2D() {
    ALGEBRA.subtract(M3, DoubleMatrix.of(3, 2, 1d, 2d, 3d, 4d, 5d, 6d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentColumnNumber2D() {
    ALGEBRA.subtract(M3, DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractWrongType1() {
    ALGEBRA.subtract(M1, M5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractWrongType2() {
    ALGEBRA.subtract(M3, M5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testSubtractWrongType3() {
    ALGEBRA.subtract(M5, M5);
  }

  @Test
  public void testSubtract() {
    Matrix m = ALGEBRA.subtract(M1, M2);
    assertTrue(m instanceof DoubleArray);
    assertMatrixEquals(m, DoubleArray.of(-2, -2));
    m = ALGEBRA.subtract(M3, M4);
    assertTrue(m instanceof DoubleMatrix);
    assertMatrixEquals(m, DoubleMatrix.of(2, 2, -4d, -4d, -4d, -4d));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testScaleWrongType() {
    ALGEBRA.scale(M5, 0.5);
  }

  @Test
  public void testScale() {
    Matrix m = ALGEBRA.scale(M1, 10);
    assertTrue(m instanceof DoubleArray);
    assertMatrixEquals(m, DoubleArray.of(10, 20));
    m = ALGEBRA.scale(m, 0.1);
    assertMatrixEquals(m, M1);
    m = ALGEBRA.scale(M3, 10);
    assertTrue(m instanceof DoubleMatrix);
    assertMatrixEquals(m, DoubleMatrix.of(2, 2, 10d, 20d, 30d, 40d));
    m = ALGEBRA.scale(m, 0.1);
    assertMatrixEquals(m, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDivide1D() {
    ALGEBRA.divide(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDivide2D() {
    ALGEBRA.divide(M3, M1);
  }

  @Test
  public void testKroneckerProduct() {
    Matrix m = ALGEBRA.kroneckerProduct(M3, M4);
    assertTrue(m instanceof DoubleMatrix);
    assertMatrixEquals(m, DoubleMatrix.of(4, 4, 5, 6, 10, 12, 7, 8, 14, 16, 15, 18, 20, 24, 21, 24, 28, 32));

  }

  private void assertMatrixEquals(final Matrix m1, final Matrix m2) {
    if (m1 instanceof DoubleArray) {
      assertTrue(m2 instanceof DoubleArray);
      final DoubleArray m3 = (DoubleArray) m1;
      final DoubleArray m4 = (DoubleArray) m2;
      assertEquals(m3.size(), m4.size());
      for (int i = 0; i < m3.size(); i++) {
        assertEquals(m3.get(i), m4.get(i), EPS);
      }
      return;
    }
    if (m2 instanceof DoubleMatrix) {
      final DoubleMatrix m3 = (DoubleMatrix) m1;
      final DoubleMatrix m4 = (DoubleMatrix) m2;
      assertEquals(m3.size(), m4.size());
      assertEquals(m3.rowCount(), m4.rowCount());
      assertEquals(m3.columnCount(), m4.columnCount());
      for (int i = 0; i < m3.rowCount(); i++) {
        for (int j = 0; j < m3.columnCount(); j++) {
          assertEquals(m3.get(i, j), m4.get(i, j), EPS);
        }
      }
    }
  }

  private static class MyMatrixAlgebra extends MatrixAlgebra {

    @Override
    public double getCondition(final Matrix m) {
      return 0;
    }

    @Override
    public double getDeterminant(final Matrix m) {
      return 0;
    }

    @Override
    public double getInnerProduct(final Matrix m1, final Matrix m2) {
      return 0;
    }

    @Override
    public DoubleMatrix getInverse(final Matrix m) {
      return null;
    }

    @Override
    public double getNorm1(final Matrix m) {
      return 0;
    }

    @Override
    public double getNorm2(final Matrix m) {
      return 0;
    }

    @Override
    public double getNormInfinity(final Matrix m) {
      return 0;
    }

    @Override
    public DoubleMatrix getOuterProduct(final Matrix m1, final Matrix m2) {
      return null;
    }

    @Override
    public DoubleMatrix getPower(final Matrix m, final int p) {
      return null;
    }

    @Override
    public double getTrace(final Matrix m) {
      return 0;
    }

    @Override
    public DoubleMatrix getTranspose(final Matrix m) {
      return null;
    }

    @Override
    public Matrix multiply(final Matrix m1, final Matrix m2) {
      return null;
    }

    @Override
    public DoubleMatrix getPower(final Matrix m, final double p) {
      return null;
    }

  }
}
