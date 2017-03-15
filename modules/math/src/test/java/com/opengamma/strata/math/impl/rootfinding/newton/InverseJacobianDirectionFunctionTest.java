/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Test.
 */
@Test
public class InverseJacobianDirectionFunctionTest {
  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  private static final InverseJacobianDirectionFunction F = new InverseJacobianDirectionFunction(ALGEBRA);
  private static final double X0 = 2.4;
  private static final double X1 = 7.6;
  private static final double X2 = 4.5;
  private static final DoubleMatrix M = DoubleMatrix.copyOf(new double[][] { {X0, 0, 0}, {0, X1, 0}, {0, 0, X2}});
  private static final DoubleArray Y = DoubleArray.of(1, 1, 1);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    new InverseJacobianDirectionFunction(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEstimate() {
    F.getDirection(null, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullY() {
    F.getDirection(M, null);
  }

  @Test
  public void test() {
    double eps = 1e-9;
    DoubleArray direction = F.getDirection(M, Y);
    assertEquals(direction.get(0), X0, eps);
    assertEquals(direction.get(1), X1, eps);
    assertEquals(direction.get(2), X2, eps);
  }
}
