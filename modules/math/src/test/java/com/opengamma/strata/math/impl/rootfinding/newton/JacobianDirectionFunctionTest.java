/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;

/**
 * Test.
 */
@Test
public class JacobianDirectionFunctionTest {

  private static final Decomposition<?> SV = DecompositionFactory.SV_COMMONS;
  private static final JacobianDirectionFunction F = new JacobianDirectionFunction(SV);
  private static final double X0 = 2.4;
  private static final double X1 = 7.6;
  private static final double X2 = 4.5;
  private static final DoubleMatrix M = DoubleMatrix.copyOf(
      new double[][] { {X0, 0, 0}, {0, X1, 0}, {0, 0, X2}});
  private static final DoubleArray Y = DoubleArray.of(1, 1, 1);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    new JacobianDirectionFunction(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEstimate() {
    F.getDirection(null, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullY() {
    F.getDirection(M, null);
  }

  public void test() {
    double eps = 1e-9;
    DoubleArray direction = F.getDirection(M, Y);
    assertEquals(direction.get(0), 1. / X0, eps);
    assertEquals(direction.get(1), 1. / X1, eps);
    assertEquals(direction.get(2), 1. / X2, eps);
  }

}
