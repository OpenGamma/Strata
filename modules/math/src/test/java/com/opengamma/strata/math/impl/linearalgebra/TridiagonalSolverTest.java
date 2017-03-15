/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static com.opengamma.strata.math.impl.linearalgebra.TridiagonalSolver.solvTriDag;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

import cern.jet.random.engine.MersenneTwister;

/**
 * Test.
 */
@Test
public class TridiagonalSolverTest {

  private static MatrixAlgebra MA = new OGMatrixAlgebra();
  private static ProbabilityDistribution<Double> RANDOM = new NormalDistribution(0, 1, new MersenneTwister(123));

  @Test
  public void test() {
    final int n = 97;
    double[] a = new double[n - 1];
    double[] b = new double[n];
    double[] c = new double[n - 1];
    double[] x = new double[n];

    for (int ii = 0; ii < n; ii++) {
      b[ii] = RANDOM.nextRandom();
      x[ii] = RANDOM.nextRandom();
      if (ii < n - 1) {
        a[ii] = RANDOM.nextRandom();
        c[ii] = RANDOM.nextRandom();
      }
    }

    final TridiagonalMatrix m = new TridiagonalMatrix(b, a, c);
    final DoubleArray xVec = DoubleArray.copyOf(x);
    final DoubleArray yVec = (DoubleArray) MA.multiply(m, xVec);

    final double[] xSolv = solvTriDag(m, yVec).toArray();

    for (int i = 0; i < n; i++) {
      assertEquals(x[i], xSolv[i], 1e-9);
    }

    DoubleArray resi = (DoubleArray) MA.subtract(MA.multiply(m, DoubleArray.copyOf(xSolv)), yVec);
    double err = MA.getNorm2(resi);
    assertEquals(0.0, err, 1e-14);

  }

}
