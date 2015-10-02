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

/**
 * Test.
 */
@Test
public class ScalarFieldFirstOrderDifferentiatorTest {

  private static final Function1D<DoubleMatrix1D, Double> F = new Function1D<DoubleMatrix1D, Double>() {

    @Override
    public Double evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      return x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
    }
  };

  private static final Function1D<DoubleMatrix1D, Boolean> DOMAIN = new Function1D<DoubleMatrix1D, Boolean>() {

    @Override
    public Boolean evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      return x1 >= 0.0 && x1 <= Math.PI;
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix1D> G = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      final double x1 = x.getEntry(0);
      final double x2 = x.getEntry(1);
      final double[] y = new double[2];
      y[0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      y[1] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      return new DoubleMatrix1D(y);
    }

  };
  private static final double EPS = 1e-4;
  private static final ScalarFieldFirstOrderDifferentiator FORWARD = new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.FORWARD, EPS);
  private static final ScalarFieldFirstOrderDifferentiator CENTRAL = new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator BACKWARD = new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.BACKWARD, EPS);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDifferenceType() {
    new ScalarFirstOrderDifferentiator(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    CENTRAL.differentiate((Function1D<DoubleMatrix1D, Double>) null);
  }

  @Test
  public void test() {
    final DoubleMatrix1D x = new DoubleMatrix1D(.2245, -1.2344);
    final DoubleMatrix1D anGrad = G.evaluate(x);
    final DoubleMatrix1D fdFwdGrad = FORWARD.differentiate(F).evaluate(x);
    final DoubleMatrix1D fdCentGrad = CENTRAL.differentiate(F).evaluate(x);
    final DoubleMatrix1D fdBackGrad = BACKWARD.differentiate(F).evaluate(x);

    for (int i = 0; i < 2; i++) {
      assertEquals(fdFwdGrad.getEntry(i), anGrad.getEntry(i), 10 * EPS);
      assertEquals(fdCentGrad.getEntry(i), anGrad.getEntry(i), EPS * EPS);
      assertEquals(fdBackGrad.getEntry(i), anGrad.getEntry(i), 10 * EPS);
    }
  }

  @Test
  public void domainTest() {
    final DoubleMatrix1D[] x = new DoubleMatrix1D[3];
    x[0] = new DoubleMatrix1D(0.2245, -1.2344);
    x[1] = new DoubleMatrix1D(0.0, 12.6);
    x[2] = new DoubleMatrix1D(Math.PI, 0.0);

    final Function1D<DoubleMatrix1D, DoubleMatrix1D> fdGradFunc = CENTRAL.differentiate(F, DOMAIN);

    for (int k = 0; k < 3; k++) {
      final DoubleMatrix1D fdRes = fdGradFunc.evaluate(x[k]);
      final DoubleMatrix1D alRes = G.evaluate(x[k]);
      for (int i = 0; i < 2; i++) {
        assertEquals(fdRes.getEntry(i), alRes.getEntry(i), 1e-7);
      }
    }
  }

}
