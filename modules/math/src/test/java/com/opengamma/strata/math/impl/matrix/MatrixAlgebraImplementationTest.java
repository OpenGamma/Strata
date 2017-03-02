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
@Test
public class MatrixAlgebraImplementationTest {

  private static final MatrixAlgebra COMMONS = MatrixAlgebraFactory.COMMONS_ALGEBRA;
  private static final MatrixAlgebra OG = MatrixAlgebraFactory.OG_ALGEBRA;
  private static final DoubleArray M1 = DoubleArray.of(1, 2);
  private static final DoubleArray M2 = DoubleArray.of(3, 4);
  private static final DoubleMatrix M3 = DoubleMatrix.copyOf(new double[][] { {1, 2}, {2, 1}});
  private static final DoubleMatrix M4 = DoubleMatrix.copyOf(new double[][] { {5, 6}, {7, 8}});
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
  public void testCommonsCondition() {
    COMMONS.getCondition(M1);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGCondition() {
    OG.getCondition(M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsDeterminant() {
    COMMONS.getCondition(M1);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGDeterminant() {
    OG.getDeterminant(M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsInnerProduct() {
    COMMONS.getInnerProduct(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGInnerProduct1() {
    OG.getInnerProduct(M1, DoubleArray.of(1, 2, 3));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGInnerProduct2() {
    OG.getInnerProduct(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsInverse() {
    COMMONS.getInverse(M1);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGInverse() {
    OG.getInverse(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsNorm1() {
    COMMONS.getNorm1(M5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGNorm1() {
    OG.getNorm1(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsNorm2() {
    COMMONS.getNorm2(M5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGNorm2_1() {
    OG.getNorm2(M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGNorm2_2() {
    OG.getNorm2(M5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsNormInfinity() {
    COMMONS.getNormInfinity(M5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGNormInfinity() {
    OG.getNormInfinity(M5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsOuterProduct() {
    COMMONS.getOuterProduct(M3, M4);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGOuterProduct() {
    OG.getOuterProduct(M3, M4);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsPower() {
    COMMONS.getPower(M1, 2);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGPower1() {
    OG.getPower(M2, 2);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testOGPower2() {
    OG.getPower(M2, 2.3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsTrace() {
    COMMONS.getTrace(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGTrace1() {
    OG.getTrace(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGTrace2() {
    OG.getTrace(DoubleMatrix.copyOf(new double[][] { {1, 2, 3}, {4, 5, 6}}));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsTranspose() {
    COMMONS.getTranspose(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGTranspose() {
    OG.getTranspose(M1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsMultiply1() {
    COMMONS.multiply(M1, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsMultiply2() {
    COMMONS.multiply(M3, M5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCommonsMultiply3() {
    COMMONS.multiply(M5, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGMultiply1() {
    OG.multiply(M5, M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGMultiply2() {
    OG.multiply(DoubleArray.of(1, 2, 3), M3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGMultiply3() {
    OG.multiply(M3, DoubleArray.of(1, 2, 3));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOGMultiply4() {
    OG.multiply(DoubleMatrix.copyOf(new double[][] { {1, 2, 3}, {4, 5, 6}}), M3);
  }

  @Test
  public void testCondition() {
    assertEquals(COMMONS.getCondition(M4), 86.9885042281285, EPS);
  }

  @Test
  public void testDeterminant() {
    assertEquals(COMMONS.getDeterminant(M4), -2.0, EPS);
  }

  @Test
  public void testNormL1() {
    assertEquals(COMMONS.getNorm1(M1), 3, EPS);
    assertEquals(COMMONS.getNorm1(M4), 14, EPS);
  }

  @Test
  public void testNormL2() {
    assertEquals(COMMONS.getNorm2(M1), 2.23606797749979, EPS);
    assertEquals(COMMONS.getNorm2(M4), 13.1900344372658, EPS);
  }

  @Test
  public void testNormLInf() {
    assertEquals(COMMONS.getNormInfinity(M1), 2, EPS);
    assertEquals(COMMONS.getNormInfinity(M4), 15, EPS);
  }

  @Test
  public void testTrace() {
    assertEquals(COMMONS.getTrace(M4), 13, EPS);
  }

  @Test
  public void testInnerProduct() {
    assertEquals(COMMONS.getInnerProduct(M1, M2), 11, EPS);
  }

  @Test
  public void testInverse() {
    assertMatrixEquals(COMMONS.getInverse(M3), DoubleMatrix.copyOf(
        new double[][] { {-0.3333333333333333, 0.6666666666666666}, {0.6666666666666666, -0.3333333333333333}}));
  }

  @Test
  public void testMultiply() {
    assertMatrixEquals(COMMONS.multiply(DoubleMatrix.identity(2), M3), M3);
    assertMatrixEquals(COMMONS.multiply(M3, M4), DoubleMatrix.copyOf(new double[][] { {19, 22}, {17, 20}}));
  }

  @Test
  public void testOuterProduct() {
    assertMatrixEquals(COMMONS.getOuterProduct(M1, M2), DoubleMatrix.copyOf(new double[][] { {3, 4}, {6, 8}}));
  }

  @Test
  public void testPower() {
    assertMatrixEquals(COMMONS.getPower(M3, 3), DoubleMatrix.copyOf(new double[][] { {13, 14}, {14, 13}}));
    assertMatrixEquals(COMMONS.getPower(M3, 3), COMMONS.multiply(M3, COMMONS.multiply(M3, M3)));
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
}
