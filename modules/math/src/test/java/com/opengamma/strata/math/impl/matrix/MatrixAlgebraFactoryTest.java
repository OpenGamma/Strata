/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Test.
 */
public class MatrixAlgebraFactoryTest {

  @Test
  public void testBadName() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MatrixAlgebraFactory.getMatrixAlgebra("X"));
  }

  @Test
  public void testBadClass() {
    assertThat(MatrixAlgebraFactory.getMatrixAlgebraName(new MatrixAlgebra() {

      @Override
      public double getCondition(Matrix m) {
        return 0;
      }

      @Override
      public double getDeterminant(Matrix m) {
        return 0;
      }

      @Override
      public double getInnerProduct(Matrix m1, Matrix m2) {
        return 0;
      }

      @Override
      public DoubleMatrix getInverse(Matrix m) {
        return null;
      }

      @Override
      public double getNorm1(Matrix m) {
        return 0;
      }

      @Override
      public double getNorm2(Matrix m) {
        return 0;
      }

      @Override
      public double getNormInfinity(Matrix m) {
        return 0;
      }

      @Override
      public DoubleMatrix getOuterProduct(Matrix m1, Matrix m2) {
        return null;
      }

      @Override
      public DoubleMatrix getPower(Matrix m, int p) {
        return null;
      }

      @Override
      public DoubleMatrix getPower(Matrix m, double p) {
        return null;
      }

      @Override
      public double getTrace(Matrix m) {
        return 0;
      }

      @Override
      public DoubleMatrix getTranspose(Matrix m) {
        return null;
      }

      @Override
      public Matrix multiply(Matrix m1, Matrix m2) {
        return null;
      }

    })).isNull();
  }

  @Test
  public void test() {
    assertThat(MatrixAlgebraFactory.getMatrixAlgebra(MatrixAlgebraFactory.COMMONS))
        .isEqualTo(MatrixAlgebraFactory.COMMONS_ALGEBRA);
    assertThat(MatrixAlgebraFactory.getMatrixAlgebra(MatrixAlgebraFactory.OG)).isEqualTo(MatrixAlgebraFactory.OG_ALGEBRA);
    assertThat(MatrixAlgebraFactory.getMatrixAlgebraName(MatrixAlgebraFactory.COMMONS_ALGEBRA))
        .isEqualTo(MatrixAlgebraFactory.COMMONS);
    assertThat(MatrixAlgebraFactory.getMatrixAlgebraName(MatrixAlgebraFactory.OG_ALGEBRA)).isEqualTo(MatrixAlgebraFactory.OG);
  }

}
