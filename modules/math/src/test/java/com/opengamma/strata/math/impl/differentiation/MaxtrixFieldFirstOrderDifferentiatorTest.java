/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public class MaxtrixFieldFirstOrderDifferentiatorTest {
  private static final MatrixFieldFirstOrderDifferentiator DIFF = new MatrixFieldFirstOrderDifferentiator();

  private static final Function<DoubleArray, DoubleMatrix> F = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(final DoubleArray x) {
      double x1 = x.get(0);
      double x2 = x.get(1);
      double[][] y = new double[3][2];
      y[0][0] = x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
      y[1][0] = 2 * x1 * x2 * Math.cos(x1 * x2) - x1 * Math.sin(x1) - x2 * Math.cos(x2);
      y[2][0] = x1 - x2;
      y[0][1] = 7.0;
      y[1][1] = Math.sin(x1);
      y[2][1] = Math.cos(x2);
      return DoubleMatrix.copyOf(y);
    }
  };

  private static final Function<DoubleArray, DoubleMatrix[]> G = new Function<DoubleArray, DoubleMatrix[]>() {

    @Override
    public DoubleMatrix[] apply(final DoubleArray x) {
      double x1 = x.get(0);
      double x2 = x.get(1);
      double[][] y = new double[3][2];
      y[0][0] = 2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1);
      y[1][0] = 2 * x2 * Math.cos(x1 * x2) - 2 * x1 * x2 * x2 * Math.sin(x1 * x2) - Math.sin(x1) - x1 * Math.cos(x1);
      y[2][0] = 1.;
      y[0][1] = 0.;
      y[1][1] = Math.cos(x1);
      y[2][1] = 0.0;
      DoubleMatrix m1 = DoubleMatrix.copyOf(y);
      y[0][0] = 4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1);
      y[1][0] = 2 * x1 * Math.cos(x1 * x2) - 2 * x1 * x1 * x2 * Math.sin(x1 * x2) - Math.cos(x2) + x2 * Math.sin(x2);
      y[2][0] = -1.;
      y[0][1] = 0.;
      y[1][1] = 0.0;
      y[2][1] = -Math.sin(x2);
      DoubleMatrix m2 = DoubleMatrix.copyOf(y);
      return new DoubleMatrix[] {m1, m2 };
    }
  };

  @Test
  public void test() {
    Function<DoubleArray, DoubleMatrix[]> analDiffFunc = DIFF.differentiate(F);

    final DoubleArray x = DoubleArray.of(1.3423, 0.235);

    DoubleMatrix[] alRes = analDiffFunc.apply(x);
    DoubleMatrix[] fdRes = G.apply(x);

    final int p = fdRes.length;
    final int n = fdRes[0].rowCount();
    final int m = fdRes[0].columnCount();
    assertEquals(p, alRes.length);
    assertEquals(n, alRes[0].rowCount());
    assertEquals(m, alRes[0].columnCount());

    for (int k = 0; k < p; k++) {
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
          assertEquals(fdRes[k].get(i, j), alRes[k].get(i, j), 1e-8);
        }
      }
    }
  }

}
