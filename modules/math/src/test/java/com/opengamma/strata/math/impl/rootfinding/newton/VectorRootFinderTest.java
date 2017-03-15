/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;
import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.rootfinding.VectorRootFinder;

/**
 * Test.
 */
@Test
public abstract class VectorRootFinderTest {
  static final double EPS = 1e-6;
  static final double TOLERANCE = 1e-8;
  static final int MAXSTEPS = 100;
  static final Function<DoubleArray, DoubleArray> LINEAR = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      final double[] data = x.toArray();
      if (data.length != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      return DoubleArray.of(
          data[0] + data[1],
          2 * data[0] - data[1] - 3.0);
    }
  };
  static final Function<DoubleArray, DoubleArray> FUNCTION2D = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      final double[] data = x.toArray();
      if (data.length != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      return DoubleArray.of(
          data[1] * Math.exp(data[0]) - Math.E,
          data[0] * data[0] + data[1] * data[1] - 2.0);
    }
  };
  static final Function<DoubleArray, DoubleMatrix> JACOBIAN2D = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(final DoubleArray x) {
      if (x.size() != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      final double[][] res = new double[2][2];
      final double temp = Math.exp(x.get(0));

      res[0][0] = x.get(1) * temp;
      res[0][1] = temp;
      for (int i = 0; i < 2; i++) {
        res[1][i] = 2 * x.get(i);
      }

      return DoubleMatrix.copyOf(res);
    }

  };

  static final Function<DoubleArray, DoubleArray> FUNCTION3D = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      if (x.size() != 3) {
        throw new IllegalArgumentException("This test is for 3-d vector only");
      }
      return DoubleArray.of(
          Math.exp(x.get(0) + x.get(1)) + x.get(2) - Math.E + 1.0,
          x.get(2) * Math.exp(x.get(0) - x.get(1)) + Math.E,
          OG_ALGEBRA.getInnerProduct(x, x) - 2.0);
    }
  };
  static final Function<DoubleArray, DoubleMatrix> JACOBIAN3D = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(final DoubleArray x) {
      if (x.size() != 3) {
        throw new IllegalArgumentException("This test is for 3-d vector only");
      }
      final double[][] res = new double[3][3];
      final double temp1 = Math.exp(x.get(0) + x.get(1));
      final double temp2 = Math.exp(x.get(0) - x.get(1));
      res[0][0] = res[0][1] = temp1;
      res[0][2] = 1.0;
      res[1][0] = x.get(2) * temp2;
      res[1][1] = -x.get(2) * temp2;
      res[1][2] = temp2;
      for (int i = 0; i < 3; i++) {
        res[2][i] = 2 * x.get(i);
      }

      return DoubleMatrix.copyOf(res);
    }

  };

  static final double[] TIME_GRID = new double[] {0.25, 0.5, 1.0, 1.5, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 20.0, 25.0, 30.0 };
  static final Function<Double, Double> DUMMY_YIELD_CURVE = new Function<Double, Double>() {

    private static final double a = -0.03;
    private static final double b = 0.02;
    private static final double c = 0.5;
    private static final double d = 0.05;

    @Override
    public Double apply(final Double x) {
      return Math.exp(-x * ((a + b * x) * Math.exp(-c * x) + d));
    }
  };
  static final Function<DoubleArray, DoubleArray> SWAP_RATES = new Function<DoubleArray, DoubleArray>() {

    private final int n = TIME_GRID.length;
    private double[] _swapRates = null;

    private void calculateSwapRates() {
      if (_swapRates != null) {
        return;
      }
      _swapRates = new double[n];
      double acc = 0.0;
      double pi;
      for (int i = 0; i < n; i++) {
        pi = DUMMY_YIELD_CURVE.apply(TIME_GRID[i]);
        acc += (TIME_GRID[i] - (i == 0 ? 0.0 : TIME_GRID[i - 1])) * pi;
        _swapRates[i] = (1.0 - pi) / acc;
      }
    }

    @Override
    public DoubleArray apply(final DoubleArray x) {
      calculateSwapRates();
      final double[] yield = x.toArray();
      final double[] diff = new double[n];
      double pi;
      double acc = 0.0;
      for (int i = 0; i < n; i++) {
        pi = Math.exp(-yield[i] * TIME_GRID[i]);
        acc += (TIME_GRID[i] - (i == 0 ? 0.0 : TIME_GRID[i - 1])) * pi;
        diff[i] = (1.0 - pi) / acc - _swapRates[i];
      }
      return DoubleArray.copyOf(diff);
    }

  };
  private static final VectorRootFinder DUMMY = new VectorRootFinder() {

    @Override
    public DoubleArray getRoot(final Function<DoubleArray, DoubleArray> function, final DoubleArray x) {
      checkInputs(function, x);
      return null;
    }

  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    DUMMY.getRoot(null, DoubleArray.EMPTY);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVector() {
    DUMMY.getRoot(LINEAR, (DoubleArray) null);
  }

  protected void assertLinear(final VectorRootFinder rootFinder, final double eps) {
    final DoubleArray x0 = DoubleArray.of(0.0, 0.0);
    final DoubleArray x1 = rootFinder.getRoot(LINEAR, x0);
    assertEquals(1.0, x1.get(0), eps);
    assertEquals(-1.0, x1.get(1), eps);
  }

  // Note: at the root (1,1) the Jacobian is singular which leads to very slow convergence and is why
  // we switch to using SVD rather than the default LU
  protected void assertFunction2D(final NewtonVectorRootFinder rootFinder, final double eps) {
    final DoubleArray x0 = DoubleArray.of(-0.0, 0.0);
    final DoubleArray x1 = rootFinder.getRoot(FUNCTION2D, JACOBIAN2D, x0);
    assertEquals(1.0, x1.get(0), eps);
    assertEquals(1.0, x1.get(1), eps);
  }

  protected void assertFunction3D(final NewtonVectorRootFinder rootFinder, final double eps) {
    final DoubleArray x0 = DoubleArray.of(0.8, 0.2, -0.7);
    final DoubleArray x1 = rootFinder.getRoot(FUNCTION3D, JACOBIAN3D, x0);
    assertEquals(1.0, x1.get(0), eps);
    assertEquals(0.0, x1.get(1), eps);
    assertEquals(-1.0, x1.get(2), eps);
  }

  protected void assertYieldCurveBootstrap(final VectorRootFinder rootFinder, final double eps) {
    final int n = TIME_GRID.length;
    final double[] flatCurve = new double[n];
    for (int i = 0; i < n; i++) {
      flatCurve[i] = 0.05;
    }
    final DoubleArray x0 = DoubleArray.copyOf(flatCurve);
    final DoubleArray x1 = rootFinder.getRoot(SWAP_RATES, x0);
    for (int i = 0; i < n; i++) {
      assertEquals(-Math.log(DUMMY_YIELD_CURVE.apply(TIME_GRID[i])) / TIME_GRID[i], x1.get(i), eps);
    }
  }

}
