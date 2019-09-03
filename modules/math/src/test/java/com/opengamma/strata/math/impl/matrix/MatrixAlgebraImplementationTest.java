/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Test.
 */
public class MatrixAlgebraImplementationTest {

  private static final MatrixAlgebra COMMONS = MatrixAlgebraFactory.COMMONS_ALGEBRA;
  private static final MatrixAlgebra OG = MatrixAlgebraFactory.OG_ALGEBRA;
  private static final DoubleArray M1 = DoubleArray.of(1, 2);
  private static final DoubleArray M2 = DoubleArray.of(3, 4);
  private static final DoubleMatrix M3 = DoubleMatrix.copyOf(new double[][] {{1, 2}, {2, 1}});
  private static final DoubleMatrix M4 = DoubleMatrix.copyOf(new double[][] {{5, 6}, {7, 8}});
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

  @Test
  public void testCommonsCondition() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getCondition(M1));
  }

  @Test
  public void testOGCondition() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getCondition(M3));
  }

  @Test
  public void testCommonsDeterminant() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getCondition(M1));
  }

  @Test
  public void testOGDeterminant() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getDeterminant(M3));
  }

  @Test
  public void testCommonsInnerProduct() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getInnerProduct(M1, M3));
  }

  @Test
  public void testOGInnerProduct1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getInnerProduct(M1, DoubleArray.of(1, 2, 3)));
  }

  @Test
  public void testOGInnerProduct2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getInnerProduct(M1, M3));
  }

  @Test
  public void testCommonsInverse() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getInverse(M1));
  }

  @Test
  public void testOGInverse() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getInverse(M1));
  }

  @Test
  public void testCommonsNorm1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getNorm1(M5));
  }

  @Test
  public void testOGNorm1() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getNorm1(M1));
  }

  @Test
  public void testCommonsNorm2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getNorm2(M5));
  }

  @Test
  public void testOGNorm2_1() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getNorm2(M3));
  }

  @Test
  public void testOGNorm2_2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getNorm2(M5));
  }

  @Test
  public void testCommonsNormInfinity() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getNormInfinity(M5));
  }

  @Test
  public void testOGNormInfinity() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getNormInfinity(M5));
  }

  @Test
  public void testCommonsOuterProduct() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getOuterProduct(M3, M4));
  }

  @Test
  public void testOGOuterProduct() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getOuterProduct(M3, M4));
  }

  @Test
  public void testCommonsPower() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getPower(M1, 2));
  }

  @Test
  public void testOGPower1() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getPower(M2, 2));
  }

  @Test
  public void testOGPower2() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> OG.getPower(M2, 2.3));
  }

  @Test
  public void testCommonsTrace() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getTrace(M1));
  }

  @Test
  public void testOGTrace1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getTrace(M1));
  }

  @Test
  public void testOGTrace2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getTrace(DoubleMatrix.copyOf(new double[][] {{1, 2, 3}, {4, 5, 6}})));
  }

  @Test
  public void testCommonsTranspose() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.getTranspose(M1));
  }

  @Test
  public void testOGTranspose() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.getTranspose(M1));
  }

  @Test
  public void testCommonsMultiply1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.multiply(M1, M3));
  }

  @Test
  public void testCommonsMultiply2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.multiply(M3, M5));
  }

  @Test
  public void testCommonsMultiply3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> COMMONS.multiply(M5, M3));
  }

  @Test
  public void testOGMultiply1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.multiply(M5, M3));
  }

  @Test
  public void testOGMultiply2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.multiply(DoubleArray.of(1, 2, 3), M3));
  }

  @Test
  public void testOGMultiply3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.multiply(M3, DoubleArray.of(1, 2, 3)));
  }

  @Test
  public void testOGMultiply4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OG.multiply(DoubleMatrix.copyOf(new double[][] {{1, 2, 3}, {4, 5, 6}}), M3));
  }

  @Test
  public void testCondition() {
    assertThat(COMMONS.getCondition(M4)).isCloseTo(86.9885042281285, offset(EPS));
  }

  @Test
  public void testDeterminant() {
    assertThat(COMMONS.getDeterminant(M4)).isCloseTo(-2.0, offset(EPS));
  }

  @Test
  public void testNormL1() {
    assertThat(COMMONS.getNorm1(M1)).isCloseTo(3, offset(EPS));
    assertThat(COMMONS.getNorm1(M4)).isCloseTo(14, offset(EPS));
  }

  @Test
  public void testNormL2() {
    assertThat(COMMONS.getNorm2(M1)).isCloseTo(2.23606797749979, offset(EPS));
    assertThat(COMMONS.getNorm2(M4)).isCloseTo(13.1900344372658, offset(EPS));
  }

  @Test
  public void testNormLInf() {
    assertThat(COMMONS.getNormInfinity(M1)).isCloseTo(2, offset(EPS));
    assertThat(COMMONS.getNormInfinity(M4)).isCloseTo(15, offset(EPS));
  }

  @Test
  public void testTrace() {
    assertThat(COMMONS.getTrace(M4)).isCloseTo(13, offset(EPS));
  }

  @Test
  public void testInnerProduct() {
    assertThat(COMMONS.getInnerProduct(M1, M2)).isCloseTo(11, offset(EPS));
  }

  @Test
  public void testInverse() {
    assertMatrixEquals(COMMONS.getInverse(M3), DoubleMatrix.copyOf(
        new double[][] {{-0.3333333333333333, 0.6666666666666666}, {0.6666666666666666, -0.3333333333333333}}));
  }

  @Test
  public void testMultiply() {
    assertMatrixEquals(COMMONS.multiply(DoubleMatrix.identity(2), M3), M3);
    assertMatrixEquals(COMMONS.multiply(M3, M4), DoubleMatrix.copyOf(new double[][] {{19, 22}, {17, 20}}));
  }

  @Test
  public void testOuterProduct() {
    assertMatrixEquals(COMMONS.getOuterProduct(M1, M2), DoubleMatrix.copyOf(new double[][] {{3, 4}, {6, 8}}));
  }

  @Test
  public void testPower() {
    assertMatrixEquals(COMMONS.getPower(M3, 3), DoubleMatrix.copyOf(new double[][] {{13, 14}, {14, 13}}));
    assertMatrixEquals(COMMONS.getPower(M3, 3), COMMONS.multiply(M3, COMMONS.multiply(M3, M3)));
  }

  private void assertMatrixEquals(final Matrix m1, final Matrix m2) {
    if (m1 instanceof DoubleArray) {
      assertThat(m2 instanceof DoubleArray).isTrue();
      final DoubleArray m3 = (DoubleArray) m1;
      final DoubleArray m4 = (DoubleArray) m2;
      assertThat(m3.size()).isEqualTo(m4.size());
      for (int i = 0; i < m3.size(); i++) {
        assertThat(m3.get(i)).isCloseTo(m4.get(i), offset(EPS));
      }
      return;
    }
    if (m2 instanceof DoubleMatrix) {
      final DoubleMatrix m3 = (DoubleMatrix) m1;
      final DoubleMatrix m4 = (DoubleMatrix) m2;
      assertThat(m3.size()).isEqualTo(m4.size());
      assertThat(m3.rowCount()).isEqualTo(m4.rowCount());
      assertThat(m3.columnCount()).isEqualTo(m4.columnCount());
      for (int i = 0; i < m3.rowCount(); i++) {
        for (int j = 0; j < m3.columnCount(); j++) {
          assertThat(m3.get(i, j)).isCloseTo(m4.get(i, j), offset(EPS));
        }
      }
    }
  }
}
