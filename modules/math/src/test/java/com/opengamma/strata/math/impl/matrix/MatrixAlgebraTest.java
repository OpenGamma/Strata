/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

/**
 * Test.
 */
@SuppressWarnings("synthetic-access")
@Test
public class MatrixAlgebraTest {
  private static final MatrixAlgebra ALGEBRA = new MyMatrixAlgebra();

  private static final DoubleMatrix1D M1 = new DoubleMatrix1D(new double[] {1, 2 });
  private static final DoubleMatrix1D M2 = new DoubleMatrix1D(new double[] {3, 4 });
  private static final DoubleMatrix2D M3 = new DoubleMatrix2D(new double[][] {new double[] {1, 2 }, new double[] {3, 4 } });
  private static final DoubleMatrix2D M4 = new DoubleMatrix2D(new double[][] {new double[] {5, 6 }, new double[] {7, 8 } });
  private static final Matrix<?> M5 = new Matrix<Double>() {

    @Override
    public Double getEntry(final int... indices) {
      return null;
    }

    @Override
    public int getNumberOfElements() {
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
    ALGEBRA.add(M1, new DoubleMatrix1D(new double[] {1, 2, 3 }));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddDifferentRowNumber2D() {
    ALGEBRA.add(M3, new DoubleMatrix2D(new double[][] {new double[] {1, 2 }, new double[] {3, 4 }, new double[] {5, 6 } }));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddDifferentColumnNumber2D() {
    ALGEBRA.add(M3, new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3 }, new double[] {4, 5, 6 } }));
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
    Matrix<?> m = ALGEBRA.add(M1, M2);
    assertTrue(m instanceof DoubleMatrix1D);
    assertMatrixEquals(m, new DoubleMatrix1D(new double[] {4, 6 }));
    m = ALGEBRA.add(M3, M4);
    assertTrue(m instanceof DoubleMatrix2D);
    assertMatrixEquals(m, new DoubleMatrix2D(new double[][] {new double[] {6, 8 }, new double[] {10, 12 } }));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractWrongSize() {
    ALGEBRA.subtract(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentRowNumber1D() {
    ALGEBRA.subtract(M1, new DoubleMatrix1D(new double[] {1, 2, 3 }));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentRowNumber2D() {
    ALGEBRA.subtract(M3, new DoubleMatrix2D(new double[][] {new double[] {1, 2 }, new double[] {3, 4 }, new double[] {5, 6 } }));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractDifferentColumnNumber2D() {
    ALGEBRA.subtract(M3, new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3 }, new double[] {4, 5, 6 } }));
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
    Matrix<?> m = ALGEBRA.subtract(M1, M2);
    assertTrue(m instanceof DoubleMatrix1D);
    assertMatrixEquals(m, new DoubleMatrix1D(new double[] {-2, -2 }));
    m = ALGEBRA.subtract(M3, M4);
    assertTrue(m instanceof DoubleMatrix2D);
    assertMatrixEquals(m, new DoubleMatrix2D(new double[][] {new double[] {-4, -4 }, new double[] {-4, -4 } }));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testScaleWrongType() {
    ALGEBRA.scale(M5, 0.5);
  }

  @Test
  public void testScale() {
    Matrix<?> m = ALGEBRA.scale(M1, 10);
    assertTrue(m instanceof DoubleMatrix1D);
    assertMatrixEquals(m, new DoubleMatrix1D(new double[] {10, 20 }));
    m = ALGEBRA.scale(m, 0.1);
    assertMatrixEquals(m, M1);
    m = ALGEBRA.scale(M3, 10);
    assertTrue(m instanceof DoubleMatrix2D);
    assertMatrixEquals(m, new DoubleMatrix2D(new double[][] {new double[] {10, 20 }, new double[] {30, 40 } }));
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
    Matrix<?> m = ALGEBRA.kroneckerProduct(M3, M4);
    assertTrue(m instanceof DoubleMatrix2D);

    assertMatrixEquals(m, new DoubleMatrix2D(new double[][] { {5, 6, 10, 12 }, {7, 8, 14, 16 }, {15, 18, 20, 24 }, {21, 24, 28, 32 } }));

  }

  private void assertMatrixEquals(final Matrix<?> m1, final Matrix<?> m2) {
    if (m1 instanceof DoubleMatrix1D) {
      assertTrue(m2 instanceof DoubleMatrix1D);
      final DoubleMatrix1D m3 = (DoubleMatrix1D) m1;
      final DoubleMatrix1D m4 = (DoubleMatrix1D) m2;
      assertEquals(m3.getNumberOfElements(), m4.getNumberOfElements());
      for (int i = 0; i < m3.getNumberOfElements(); i++) {
        assertEquals(m3.getEntry(i), m4.getEntry(i), EPS);
      }
      return;
    }
    if (m2 instanceof DoubleMatrix2D) {
      final DoubleMatrix2D m3 = (DoubleMatrix2D) m1;
      final DoubleMatrix2D m4 = (DoubleMatrix2D) m2;
      assertEquals(m3.getNumberOfElements(), m4.getNumberOfElements());
      assertEquals(m3.getNumberOfRows(), m4.getNumberOfRows());
      assertEquals(m3.getNumberOfColumns(), m4.getNumberOfColumns());
      for (int i = 0; i < m3.getNumberOfRows(); i++) {
        for (int j = 0; j < m3.getNumberOfColumns(); j++) {
          assertEquals(m3.getEntry(i, j), m4.getEntry(i, j), EPS);
        }
      }
    }
  }

  private static class MyMatrixAlgebra extends MatrixAlgebra {

    @Override
    public double getCondition(final Matrix<?> m) {
      return 0;
    }

    @Override
    public double getDeterminant(final Matrix<?> m) {
      return 0;
    }

    @Override
    public double getInnerProduct(final Matrix<?> m1, final Matrix<?> m2) {
      return 0;
    }

    @Override
    public DoubleMatrix2D getInverse(final Matrix<?> m) {
      return null;
    }

    @Override
    public double getNorm1(final Matrix<?> m) {
      return 0;
    }

    @Override
    public double getNorm2(final Matrix<?> m) {
      return 0;
    }

    @Override
    public double getNormInfinity(final Matrix<?> m) {
      return 0;
    }

    @Override
    public DoubleMatrix2D getOuterProduct(final Matrix<?> m1, final Matrix<?> m2) {
      return null;
    }

    @Override
    public DoubleMatrix2D getPower(final Matrix<?> m, final int p) {
      return null;
    }

    @Override
    public double getTrace(final Matrix<?> m) {
      return 0;
    }

    @Override
    public DoubleMatrix2D getTranspose(final Matrix<?> m) {
      return null;
    }

    @Override
    public Matrix<?> multiply(final Matrix<?> m1, final Matrix<?> m2) {
      return null;
    }

    @Override
    public DoubleMatrix2D getPower(final Matrix<?> m, final double p) {
      return null;
    }

  }
}
