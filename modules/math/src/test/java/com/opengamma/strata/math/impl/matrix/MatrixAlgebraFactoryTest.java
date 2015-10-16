/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Test.
 */
@Test
public class MatrixAlgebraFactoryTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadName() {
    MatrixAlgebraFactory.getMatrixAlgebra("X");
  }

  public void testBadClass() {
    assertNull(MatrixAlgebraFactory.getMatrixAlgebraName(new MatrixAlgebra() {

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

    }));
  }

  public void test() {
    assertEquals(MatrixAlgebraFactory.getMatrixAlgebra(MatrixAlgebraFactory.COMMONS), MatrixAlgebraFactory.COMMONS_ALGEBRA);
    assertEquals(MatrixAlgebraFactory.getMatrixAlgebra(MatrixAlgebraFactory.OG), MatrixAlgebraFactory.OG_ALGEBRA);
    assertEquals(MatrixAlgebraFactory.getMatrixAlgebraName(MatrixAlgebraFactory.COMMONS_ALGEBRA), MatrixAlgebraFactory.COMMONS);
    assertEquals(MatrixAlgebraFactory.getMatrixAlgebraName(MatrixAlgebraFactory.OG_ALGEBRA), MatrixAlgebraFactory.OG);
  }

}
