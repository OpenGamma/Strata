/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public class MaxtrixFieldFirstOrderDifferentiatorTest {
  private static final MatrixFieldFirstOrderDifferentiator DIFF = new MatrixFieldFirstOrderDifferentiator();

  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D> F = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

    @Override
    public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
      double x1 = x.getEntry(0);
      double x2 = x.getEntry(1);
      double[][] y = new double[3][2];
      y[0][0] = x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
      y[1][0] = 2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2);
      y[2][0] = x1 - x2;
      y[0][1] = 7.0;
      y[1][1] = Math.sin(x1);
      y[2][1] = Math.cos(x2);
      return new DoubleMatrix2D(y);
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D[]> G = new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {

    @Override
    public DoubleMatrix2D[] evaluate(final DoubleMatrix1D x) {
      double x1 = x.getEntry(0);
      double x2 = x.getEntry(1);
      double[][] y = new double[3][2];
      y[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      y[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      y[2][0] = 1.;
      y[0][1] = 0.;
      y[1][1] = Math.cos(x1);
      y[2][1] = 0.0;
      DoubleMatrix2D m1 = new DoubleMatrix2D(y);
      y[0][0] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      y[1][0] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      y[2][0] = -1.;
      y[0][1] = 0.;
      y[1][1] = 0.0;
      y[2][1] = -Math.sin(x2);
      DoubleMatrix2D m2 = new DoubleMatrix2D(y);
      return new DoubleMatrix2D[] {m1, m2 };
    }
  };

  @Test
  public void test() {
    Function1D<DoubleMatrix1D, DoubleMatrix2D[]> analDiffFunc = DIFF.differentiate(F);

    final DoubleMatrix1D x = new DoubleMatrix1D(new double[] {1.3423, 0.235 });

    DoubleMatrix2D[] alRes = analDiffFunc.evaluate(x);
    DoubleMatrix2D[] fdRes = G.evaluate(x);

    final int p = fdRes.length;
    final int n = fdRes[0].getNumberOfRows();
    final int m = fdRes[0].getNumberOfColumns();
    assertEquals(p, alRes.length);
    assertEquals(n, alRes[0].getNumberOfRows());
    assertEquals(m, alRes[0].getNumberOfColumns());

    for (int k = 0; k < p; k++) {
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
          assertEquals(fdRes[k].getEntry(i, j), alRes[k].getEntry(i, j), 1e-8);
        }
      }
    }
  }

}
