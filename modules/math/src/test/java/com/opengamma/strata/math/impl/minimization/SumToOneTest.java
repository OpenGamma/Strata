/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquare;

/**
 * Test.
 */
public class SumToOneTest {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final NonLinearLeastSquare SOLVER = new NonLinearLeastSquare(DecompositionFactory.SV_COMMONS, MA, 1e-9);
  private static final VectorFieldFirstOrderDifferentiator DIFFER = new VectorFieldFirstOrderDifferentiator();
  private static final Well44497b RANDOM = new Well44497b(0L);

  @Test
  public void setTest() {
    int n = 7;
    int[][] sets = SumToOne.getSet(n);
    assertThat(n).isEqualTo(sets.length);
  }

  @Test
  public void setTest2() {
    int n = 13;
    int[][] sets = SumToOne.getSet(n);
    assertThat(n).isEqualTo(sets.length);
  }

  @Test
  public void transformTest() {
    for (int n = 2; n < 13; n++) {
      double[] from = new double[n - 1];
      for (int j = 0; j < n - 1; j++) {
        from[j] = RANDOM.nextDouble() * Math.PI / 2;
      }
      SumToOne trans = new SumToOne(n);
      DoubleArray to = trans.transform(DoubleArray.copyOf(from));
      assertThat(n).isEqualTo(to.size());
      double sum = 0;
      for (int i = 0; i < n; i++) {
        sum += to.get(i);
      }
      assertThat(sum).as("vector length " + n).isCloseTo(1.0, offset(1e-9));
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
      DoubleArray w = trans.transform(DoubleArray.copyOf(theta));

      DoubleArray theta2 = trans.inverseTransform(w);
      for (int j = 0; j < n - 1; j++) {
        assertThat(theta[j]).as("element " + j + ", of vector length " + n).isCloseTo(theta2.get(j), offset(1e-9));
      }
    }
  }

  @Test
  public void solverTest() {
    double[] w = new double[] {0.01, 0.5, 0.3, 0.19};
    final int n = w.length;
    final SumToOne trans = new SumToOne(n);
    Function<DoubleArray, DoubleArray> func = new Function<DoubleArray, DoubleArray>() {

      @Override
      public DoubleArray apply(DoubleArray theta) {
        return trans.transform(theta);
      }
    };

    DoubleArray sigma = DoubleArray.filled(n, 1e-4);
    DoubleArray start = DoubleArray.filled(n - 1, 0.8);

    LeastSquareResults res = SOLVER.solve(DoubleArray.copyOf(w), sigma, func, start/*, maxJump*/);
    assertThat(res.getChiSq()).as("chi sqr").isCloseTo(0.0, offset(1e-9));
    double[] fit = res.getFitParameters().toArray();
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

      assertThat(expected[i]).isCloseTo(x, offset(1e-9));
    }

  }

  @Test
  public void solverTest2() {
    double[] w = new double[] {3.0, 4.0};
    final int n = w.length;
    Function<DoubleArray, DoubleArray> func = new Function<DoubleArray, DoubleArray>() {

      @Override
      public DoubleArray apply(DoubleArray x) {
        double a = x.get(0);
        double theta = x.get(1);
        double c1 = Math.cos(theta);
        return DoubleArray.of(
            a * c1 * c1,
            a * (1 - c1 * c1));
      }
    };

    DoubleArray sigma = DoubleArray.filled(n, 1e-4);
    DoubleArray start = DoubleArray.of(0.0, 0.8);

    LeastSquareResults res = SOLVER.solve(DoubleArray.copyOf(w), sigma, func, start/*, maxJump*/);
    assertThat(res.getChiSq()).as("chi sqr").isCloseTo(0.0, offset(1e-9));
    double[] fit = res.getFitParameters().toArray();
    assertThat(7.0).isCloseTo(fit[0], offset(1e-9));
    assertThat(Math.atan(Math.sqrt(4 / 3.))).isCloseTo(fit[1], offset(1e-9));
  }

  @Test
  public void jacobianTest() {
    final int n = 5;

    final SumToOne trans = new SumToOne(n);
    Function<DoubleArray, DoubleArray> func = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray theta) {
        return trans.transform(theta);
      }
    };

    Function<DoubleArray, DoubleMatrix> jacFunc = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray theta) {
        return trans.jacobian(theta);
      }
    };

    Function<DoubleArray, DoubleMatrix> fdJacFunc = DIFFER.differentiate(func);

    for (int tries = 0; tries < 10; tries++) {
      DoubleArray vTheta = DoubleArray.of(n - 1, i -> RANDOM.nextDouble());
      DoubleMatrix jac = jacFunc.apply(vTheta);
      DoubleMatrix fdJac = fdJacFunc.apply(vTheta);
      for (int j = 0; j < n - 1; j++) {
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
          sum += jac.get(i, j);
          assertThat(jac.get(i, j)).as("element " + i + " " + j).isCloseTo(fdJac.get(i, j), offset(1e-6));
        }
        assertThat(sum).as("wrong sum of sensitivities").isCloseTo(0.0, offset(1e-15));
      }

    }
  }
}
