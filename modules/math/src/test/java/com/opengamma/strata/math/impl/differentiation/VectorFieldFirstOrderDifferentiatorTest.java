/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class VectorFieldFirstOrderDifferentiatorTest {

  private static final Function<DoubleArray, DoubleArray> F = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      double x1 = x.get(0);
      double x2 = x.get(1);
      return DoubleArray.of(
          x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1),
          2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2));
    }
  };

  private static final Function<DoubleArray, DoubleArray> F2 = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      double x1 = x.get(0);
      double x2 = x.get(1);
      return DoubleArray.of(
          x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1),
          2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2),
          x1 - x2);
    }
  };

  private static final Function<DoubleArray, DoubleMatrix> G = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(final DoubleArray x) {
      final double x1 = x.get(0);
      final double x2 = x.get(1);
      final double[][] jac = new double[2][2];
      jac[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      jac[0][1] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      jac[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      jac[1][1] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      return DoubleMatrix.copyOf(jac);
    }
  };

  private static final Function<DoubleArray, DoubleMatrix> G2 = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(final DoubleArray x) {
      final double x1 = x.get(0);
      final double x2 = x.get(1);
      final double[][] jac = new double[3][2];
      jac[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      jac[0][1] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      jac[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      jac[1][1] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      jac[2][0] = 1;
      jac[2][1] = -1;
      return DoubleMatrix.copyOf(jac);
    }
  };

  private static final Function<DoubleArray, Boolean> DOMAIN = new Function<DoubleArray, Boolean>() {

    @Override
    public Boolean apply(final DoubleArray x) {
      final double x1 = x.get(0);
      final double x2 = x.get(1);
      if (x1 < 0 || x1 > Math.PI || x2 < 0 || x2 > Math.PI) {
        return false;
      }
      return true;
    }

  };

  private static final double EPS = 1e-5;
  private static final VectorFieldFirstOrderDifferentiator FORWARD = new VectorFieldFirstOrderDifferentiator(FiniteDifferenceType.FORWARD, EPS);
  private static final VectorFieldFirstOrderDifferentiator CENTRAL = new VectorFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final VectorFieldFirstOrderDifferentiator BACKWARD = new VectorFieldFirstOrderDifferentiator(FiniteDifferenceType.BACKWARD, EPS);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDifferenceType() {
    new ScalarFirstOrderDifferentiator(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    CENTRAL.differentiate((Function<DoubleArray, DoubleArray>) null);
  }

  @Test
  public void test() {
    final DoubleArray x = DoubleArray.of(.2245, -1.2344);
    final DoubleMatrix anJac = G.apply(x);
    final DoubleMatrix fdFwdJac = FORWARD.differentiate(F).apply(x);
    final DoubleMatrix fdCentGrad = CENTRAL.differentiate(F).apply(x);
    final DoubleMatrix fdBackGrad = BACKWARD.differentiate(F).apply(x);

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals(fdFwdJac.get(i, j), anJac.get(i, j), 10 * EPS);
        assertEquals(fdCentGrad.get(i, j), anJac.get(i, j), 10 * EPS * EPS);
        assertEquals(fdBackGrad.get(i, j), anJac.get(i, j), 10 * EPS);
      }
    }
  }

  @Test
  public void test2() {
    final DoubleArray x = DoubleArray.of(1.3423, 0.235);
    final DoubleMatrix anJac = G2.apply(x);
    final DoubleMatrix fdFwdJac = FORWARD.differentiate(F2).apply(x);
    final DoubleMatrix fdCentGrad = CENTRAL.differentiate(F2).apply(x);
    final DoubleMatrix fdBackGrad = BACKWARD.differentiate(F2).apply(x);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals(fdFwdJac.get(i, j), anJac.get(i, j), 10 * EPS);
        assertEquals(fdCentGrad.get(i, j), anJac.get(i, j), 10 * EPS * EPS);
        assertEquals(fdBackGrad.get(i, j), anJac.get(i, j), 10 * EPS);
      }
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void outsideDomainTest() {
    final Function<DoubleArray, DoubleMatrix> fdJacFunc = CENTRAL.differentiate(F2, DOMAIN);
    fdJacFunc.apply(DoubleArray.of(2.3, 3.2));
  }

  @Test
  public void testDomain() {
    final DoubleArray[] x = new DoubleArray[7];

    x[0] = DoubleArray.of(1.3423, 0.235);
    x[1] = DoubleArray.of(0.0, 1.235);
    x[2] = DoubleArray.of(Math.PI, 3.1);
    x[3] = DoubleArray.of(2.3, 0.0);
    x[4] = DoubleArray.of(2.3, Math.PI);
    x[5] = DoubleArray.of(0.0, 0.0);
    x[6] = DoubleArray.of(Math.PI, Math.PI);

    final Function<DoubleArray, DoubleMatrix> fdJacFunc = CENTRAL.differentiate(F2, DOMAIN);

    for (int k = 0; k < 7; k++) {
      final DoubleMatrix anJac = G2.apply(x[k]);
      final DoubleMatrix fdJac = fdJacFunc.apply(x[k]);

      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 2; j++) {
          assertEquals("set " + k, anJac.get(i, j), fdJac.get(i, j), 1e-8);
        }
      }
    }
  }

}
