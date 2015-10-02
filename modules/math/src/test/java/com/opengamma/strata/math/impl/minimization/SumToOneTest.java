/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquare;

/**
 * Test.
 */
@Test
public class SumToOneTest {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final NonLinearLeastSquare SOLVER = new NonLinearLeastSquare(DecompositionFactory.SV_COMMONS, MA, 1e-9);
  private static final VectorFieldFirstOrderDifferentiator DIFFER = new VectorFieldFirstOrderDifferentiator();
  private static final Well44497b RANDOM = new Well44497b(0L);

  @Test
  public void setTest() {
    int n = 7;
    int[][] sets = SumToOne.getSet(n);
    assertEquals(n, sets.length);
  }

  @Test
  public void setTest2() {
    int n = 13;
    int[][] sets = SumToOne.getSet(n);
    assertEquals(n, sets.length);
  }

  @Test
  public void transformTest() {
    for (int n = 2; n < 13; n++) {
      double[] from = new double[n - 1];
      for (int j = 0; j < n - 1; j++) {
        from[j] = RANDOM.nextDouble() * Math.PI / 2;
      }
      SumToOne trans = new SumToOne(n);
      DoubleMatrix1D to = trans.transform(new DoubleMatrix1D(from));
      assertEquals(n, to.getNumberOfElements());
      double sum = 0;
      for (int i = 0; i < n; i++) {
        sum += to.getEntry(i);
      }
      assertEquals("vector length " + n, 1.0, sum, 1e-9);
    }
  }

  @Test
  public void inverseTransformTest() {
    for (int n = 2; n < 13; n++) {
      double[] theta = new double[n - 1];
      for (int j = 0; j < n - 1; j++) {
        theta[j] = RANDOM.nextDouble() * Math.PI / 2;
      }
      SumToOne trans = new SumToOne(n);
      DoubleMatrix1D w = trans.transform(new DoubleMatrix1D(theta));

      DoubleMatrix1D theta2 = trans.inverseTransform(w);
      for (int j = 0; j < n - 1; j++) {
        assertEquals("element " + j + ", of vector length " + n, theta[j], theta2.getEntry(j), 1e-9);
      }
    }
  }

  @Test
  public void solverTest() {
    double[] w = new double[] {0.01, 0.5, 0.3, 0.19 };
    final int n = w.length;
    final SumToOne trans = new SumToOne(n);
    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D theta) {
        return trans.transform(theta);
      }
    };

    DoubleMatrix1D sigma = new DoubleMatrix1D(n, 1e-4);
    DoubleMatrix1D start = new DoubleMatrix1D(n - 1, 0.8);

    LeastSquareResults res = SOLVER.solve(new DoubleMatrix1D(w), sigma, func, start/*, maxJump*/);
    assertEquals("chi sqr", 0.0, res.getChiSq(), 1e-9);
    double[] fit = res.getFitParameters().getData();
    double[] expected = trans.inverseTransform(w);
    for (int i = 0; i < n - 1; i++) {
      //put the fit result back in the range 0 - pi/2
      double x = fit[i];
      if (x < 0) {
        x = -x;
      }
      if (x > Math.PI / 2) {
        int p = (int) (x / Math.PI);
        x -= p * Math.PI;
        if (x > Math.PI / 2) {
          x = -x + Math.PI;
        }
      }

      assertEquals(expected[i], x, 1e-9);
    }

  }

  @Test
  public void solverTest2() {
    double[] w = new double[] {3.0, 4.0 };
    final int n = w.length;
    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        double a = x.getEntry(0);
        double theta = x.getEntry(1);
        double[] temp = new double[2];
        double c1 = Math.cos(theta);
        temp[0] = a * c1 * c1;
        temp[1] = a * (1 - c1 * c1);
        return new DoubleMatrix1D(temp);
      }
    };

    DoubleMatrix1D sigma = new DoubleMatrix1D(n, 1e-4);
    DoubleMatrix1D start = new DoubleMatrix1D(0.0, 0.8);

    LeastSquareResults res = SOLVER.solve(new DoubleMatrix1D(w), sigma, func, start/*, maxJump*/);
    assertEquals("chi sqr", 0.0, res.getChiSq(), 1e-9);
    double[] fit = res.getFitParameters().getData();
    assertEquals(7.0, fit[0], 1e-9);
    assertEquals(Math.atan(Math.sqrt(4 / 3.)), fit[1], 1e-9);
  }

  @Test
  public void jacobianTest() {
    final int n = 5;

    final SumToOne trans = new SumToOne(n);
    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D theta) {
        return trans.transform(theta);
      }
    };

    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFunc = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D theta) {
        return trans.jacobian(theta);
      }
    };

    Function1D<DoubleMatrix1D, DoubleMatrix2D> fdJacFunc = DIFFER.differentiate(func);

    double[] theta = new double[n - 1];
    for (int tries = 0; tries < 10; tries++) {

      for (int i = 0; i < n - 1; i++) {
        theta[i] = RANDOM.nextDouble();
      }
      DoubleMatrix1D vTheta = new DoubleMatrix1D(theta);
      DoubleMatrix2D jac = jacFunc.evaluate(vTheta);
      DoubleMatrix2D fdJac = fdJacFunc.evaluate(vTheta);
      for (int j = 0; j < n - 1; j++) {
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
          sum += jac.getEntry(i, j);
          assertEquals("element " + i + " " + j, fdJac.getEntry(i, j), jac.getEntry(i, j), 1e-6);
        }
        assertEquals("wrong sum of sensitivities", 0.0, sum, 1e-15);
      }

    }
  }
}
