/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Test.
 */
@Test
public class VectorFieldFirstOrderDifferentiatorTest {

  private static final Function1D<DoubleMatrix1D, DoubleMatrix1D> F = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      final double[] y = new double[2];
      y[0] = x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
      y[1] = 2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2);
      return new DoubleMatrix1D(y);
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix1D> F2 = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      final double[] y = new double[3];
      y[0] = x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
      y[1] = 2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2);
      y[2] = x1 - x2;
      return new DoubleMatrix1D(y);
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D> G = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

    @Override
    public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      final double[][] jac = new double[2][2];
      jac[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      jac[0][1] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      jac[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      jac[1][1] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      return new DoubleMatrix2D(jac);
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D> G2 = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

    @Override
    public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      final double[][] jac = new double[3][2];
      jac[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      jac[0][1] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      jac[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      jac[1][1] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      jac[2][0] = 1;
      jac[2][1] = -1;
      return new DoubleMatrix2D(jac);
    }
  };

  private static final Function1D<DoubleMatrix1D, Boolean> DOMAIN = new Function1D<DoubleMatrix1D, Boolean>() {

    @Override
    public Boolean evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
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
    CENTRAL.differentiate((Function1D<DoubleMatrix1D, DoubleMatrix1D>) null);
  }

  @Test
  public void test() {
    final DoubleMatrix1D x = new DoubleMatrix1D(new double[] {.2245, -1.2344 });
    final DoubleMatrix2D anJac = G.evaluate(x);
    final DoubleMatrix2D fdFwdJac = FORWARD.differentiate(F).evaluate(x);
    final DoubleMatrix2D fdCentGrad = CENTRAL.differentiate(F).evaluate(x);
    final DoubleMatrix2D fdBackGrad = BACKWARD.differentiate(F).evaluate(x);

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals(fdFwdJac.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS);
        assertEquals(fdCentGrad.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS * EPS);
        assertEquals(fdBackGrad.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS);
      }
    }
  }

  @Test
  public void test2() {
    final DoubleMatrix1D x = new DoubleMatrix1D(new double[] {1.3423, 0.235 });
    final DoubleMatrix2D anJac = G2.evaluate(x);
    final DoubleMatrix2D fdFwdJac = FORWARD.differentiate(F2).evaluate(x);
    final DoubleMatrix2D fdCentGrad = CENTRAL.differentiate(F2).evaluate(x);
    final DoubleMatrix2D fdBackGrad = BACKWARD.differentiate(F2).evaluate(x);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals(fdFwdJac.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS);
        assertEquals(fdCentGrad.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS * EPS);
        assertEquals(fdBackGrad.getEntry(i, j), anJac.getEntry(i, j), 10 * EPS);
      }
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void outsideDomainTest() {
    final Function1D<DoubleMatrix1D, DoubleMatrix2D> fdJacFunc = CENTRAL.differentiate(F2, DOMAIN);
    fdJacFunc.evaluate(new DoubleMatrix1D(2.3, 3.2));
  }

  @Test
  public void testDomain() {
    final DoubleMatrix1D[] x = new DoubleMatrix1D[7];

    x[0] = new DoubleMatrix1D(new double[] {1.3423, 0.235 });
    x[1] = new DoubleMatrix1D(new double[] {0.0, 1.235 });
    x[2] = new DoubleMatrix1D(new double[] {Math.PI, 3.1 });
    x[3] = new DoubleMatrix1D(new double[] {2.3, 0.0 });
    x[4] = new DoubleMatrix1D(new double[] {2.3, Math.PI });
    x[5] = new DoubleMatrix1D(new double[] {0.0, 0.0 });
    x[6] = new DoubleMatrix1D(new double[] {Math.PI, Math.PI });

    final Function1D<DoubleMatrix1D, DoubleMatrix2D> fdJacFunc = CENTRAL.differentiate(F2, DOMAIN);

    for (int k = 0; k < 7; k++) {
      final DoubleMatrix2D anJac = G2.evaluate(x[k]);
      final DoubleMatrix2D fdJac = fdJacFunc.evaluate(x[k]);

      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 2; j++) {
          assertEquals("set " + k, anJac.getEntry(i, j), fdJac.getEntry(i, j), 1e-8);
        }
      }
    }
  }

}
