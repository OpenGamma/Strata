/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.rootfinding.VectorRootFinder;

/**
 * Test.
 */
@Test
public abstract class VectorRootFinderTest {
  static final double EPS = 1e-6;
  static final double TOLERANCE = 1e-8;
  static final int MAXSTEPS = 100;
  static final Function1D<DoubleMatrix1D, DoubleMatrix1D> LINEAR = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      final double[] data = x.getData();
      if (data.length != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      final double[] res = new double[2];
      res[0] = data[0] + data[1];
      res[1] = 2 * data[0] - data[1] - 3.0;
      return new DoubleMatrix1D(res);
    }
  };
  static final Function1D<DoubleMatrix1D, DoubleMatrix1D> FUNCTION2D = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      final double[] data = x.getData();
      if (data.length != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      final double[] res = new double[2];
      res[0] = data[1] * Math.exp(data[0]) - Math.E;
      res[1] = data[0] * data[0] + data[1] * data[1] - 2.0;
      return new DoubleMatrix1D(res);
    }
  };
  static final Function1D<DoubleMatrix1D, DoubleMatrix2D> JACOBIAN2D = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

    @Override
    public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
      if (x.getNumberOfElements() != 2) {
        throw new IllegalArgumentException("This test is for 2-d vector only");
      }
      final double[][] res = new double[2][2];
      final double temp = Math.exp(x.getEntry(0));

      res[0][0] = x.getEntry(1) * temp;
      res[0][1] = temp;
      for (int i = 0; i < 2; i++) {
        res[1][i] = 2 * x.getEntry(i);
      }

      return new DoubleMatrix2D(res);
    }

  };

  static final Function1D<DoubleMatrix1D, DoubleMatrix1D> FUNCTION3D = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      if (x.getNumberOfElements() != 3) {
        throw new IllegalArgumentException("This test is for 3-d vector only");
      }
      final double[] res = new double[3];
      res[0] = Math.exp(x.getEntry(0) + x.getEntry(1)) + x.getEntry(2) - Math.E + 1.0;
      res[1] = x.getEntry(2) * Math.exp(x.getEntry(0) - x.getEntry(1)) + Math.E;
      res[2] = OG_ALGEBRA.getInnerProduct(x, x) - 2.0;
      return new DoubleMatrix1D(res);
    }
  };
  static final Function1D<DoubleMatrix1D, DoubleMatrix2D> JACOBIAN3D = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

    @Override
    public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
      if (x.getNumberOfElements() != 3) {
        throw new IllegalArgumentException("This test is for 3-d vector only");
      }
      final double[][] res = new double[3][3];
      final double temp1 = Math.exp(x.getEntry(0) + x.getEntry(1));
      final double temp2 = Math.exp(x.getEntry(0) - x.getEntry(1));
      res[0][0] = res[0][1] = temp1;
      res[0][2] = 1.0;
      res[1][0] = x.getEntry(2) * temp2;
      res[1][1] = -x.getEntry(2) * temp2;
      res[1][2] = temp2;
      for (int i = 0; i < 3; i++) {
        res[2][i] = 2 * x.getEntry(i);
      }

      return new DoubleMatrix2D(res);
    }

  };

  static final double[] TIME_GRID = new double[] {0.25, 0.5, 1.0, 1.5, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 20.0, 25.0, 30.0 };
  static final Function1D<Double, Double> DUMMY_YIELD_CURVE = new Function1D<Double, Double>() {

    private static final double a = -0.03;
    private static final double b = 0.02;
    private static final double c = 0.5;
    private static final double d = 0.05;

    @Override
    public Double evaluate(final Double x) {
      return Math.exp(-x * ((a + b * x) * Math.exp(-c * x) + d));
    }
  };
  static final Function1D<DoubleMatrix1D, DoubleMatrix1D> SWAP_RATES = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

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
        pi = DUMMY_YIELD_CURVE.evaluate(TIME_GRID[i]);
        acc += (TIME_GRID[i] - (i == 0 ? 0.0 : TIME_GRID[i - 1])) * pi;
        _swapRates[i] = (1.0 - pi) / acc;
      }
    }

    @Override
    public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
      calculateSwapRates();
      final double[] yield = x.getData();
      final double[] diff = new double[n];
      double pi;
      double acc = 0.0;
      for (int i = 0; i < n; i++) {
        pi = Math.exp(-yield[i] * TIME_GRID[i]);
        acc += (TIME_GRID[i] - (i == 0 ? 0.0 : TIME_GRID[i - 1])) * pi;
        diff[i] = (1.0 - pi) / acc - _swapRates[i];
      }

      return new DoubleMatrix1D(diff);
    }

  };
  private static final VectorRootFinder DUMMY = new VectorRootFinder() {

    @Override
    public DoubleMatrix1D getRoot(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DoubleMatrix1D x) {
      checkInputs(function, x);
      return null;
    }

  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    DUMMY.getRoot(null, new DoubleMatrix1D(new double[0]));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVector() {
    DUMMY.getRoot(LINEAR, (DoubleMatrix1D) null);
  }

  protected void assertLinear(final VectorRootFinder rootFinder, final double eps) {
    final DoubleMatrix1D x0 = new DoubleMatrix1D(0.0, 0.0);
    final DoubleMatrix1D x1 = rootFinder.getRoot(LINEAR, x0);
    assertEquals(1.0, x1.getData()[0], eps);
    assertEquals(-1.0, x1.getData()[1], eps);
  }

  // Note: at the root (1,1) the Jacobian is singular which leads to very slow convergence and is why
  // we switch to using SVD rather than the default LU
  protected void assertFunction2D(final NewtonVectorRootFinder rootFinder, final double eps) {
    final DoubleMatrix1D x0 = new DoubleMatrix1D(-0.0, 0.0);
    final DoubleMatrix1D x1 = rootFinder.getRoot(FUNCTION2D, JACOBIAN2D, x0);
    assertEquals(1.0, x1.getEntry(0), eps);
    assertEquals(1.0, x1.getEntry(1), eps);
  }

  protected void assertFunction3D(final NewtonVectorRootFinder rootFinder, final double eps) {
    final DoubleMatrix1D x0 = new DoubleMatrix1D(0.8, 0.2, -0.7);
    final DoubleMatrix1D x1 = rootFinder.getRoot(FUNCTION3D, JACOBIAN3D, x0);
    assertEquals(1.0, x1.getData()[0], eps);
    assertEquals(0.0, x1.getData()[1], eps);
    assertEquals(-1.0, x1.getData()[2], eps);
  }

  protected void assertYieldCurveBootstrap(final VectorRootFinder rootFinder, final double eps) {
    final int n = TIME_GRID.length;
    final double[] flatCurve = new double[n];
    for (int i = 0; i < n; i++) {
      flatCurve[i] = 0.05;
    }
    final DoubleMatrix1D x0 = new DoubleMatrix1D(flatCurve);
    final DoubleMatrix1D x1 = rootFinder.getRoot(SWAP_RATES, x0);
    for (int i = 0; i < n; i++) {
      assertEquals(-Math.log(DUMMY_YIELD_CURVE.evaluate(TIME_GRID[i])) / TIME_GRID[i], x1.getData()[i], eps);
    }
  }

}
