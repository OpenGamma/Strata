/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.ParameterizedFunction;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;

/**
 * Test.
 */
@Test
public class NonLinearLeastSquareTest {
  private static final NormalDistribution NORMAL = new NormalDistribution(0, 1.0, new MersenneTwister64(MersenneTwister.DEFAULT_SEED));
  private static final DoubleArray X;
  private static final DoubleArray Y;
  private static final DoubleArray SIGMA;
  private static final NonLinearLeastSquare LS;

  private static final Function<Double, Double> TARGET = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return Math.sin(x);
    }
  };

  private static final Function<DoubleArray, DoubleArray> FUNCTION = new Function<DoubleArray, DoubleArray>() {

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleArray apply(final DoubleArray a) {
      ArgChecker.isTrue(a.size() == 4, "four parameters");
      final int n = X.size();
      final double[] res = new double[n];
      for (int i = 0; i < n; i++) {
        res[i] = a.get(0) * Math.sin(a.get(1) * X.get(i) + a.get(2)) + a.get(3);
      }
      return DoubleArray.copyOf(res);
    }
  };

  private static final ParameterizedFunction<Double, DoubleArray, Double> PARAM_FUNCTION = new ParameterizedFunction<Double, DoubleArray, Double>() {

    @Override
    public Double evaluate(final Double x, final DoubleArray a) {
      ArgChecker.isTrue(a.size() == getNumberOfParameters(), "four parameters");
      return a.get(0) * Math.sin(a.get(1) * x + a.get(2)) + a.get(3);
    }

    @Override
    public int getNumberOfParameters() {
      return 4;
    }
  };

  private static final ParameterizedFunction<Double, DoubleArray, DoubleArray> PARAM_GRAD = new ParameterizedFunction<Double, DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray evaluate(final Double x, final DoubleArray a) {
      ArgChecker.isTrue(a.size() == getNumberOfParameters(), "four parameters");
      final double temp1 = Math.sin(a.get(1) * x + a.get(2));
      final double temp2 = Math.cos(a.get(1) * x + a.get(2));
          final double[] res = new double[4];
          res[0] = temp1;
          res[2] = a.get(0) * temp2;
          res[1] = x * res[2];
          res[3] = 1.0;
          return DoubleArray.copyOf(res);
    }

    @Override
    public int getNumberOfParameters() {
      return 4;
    }
  };

  private static final Function<DoubleArray, DoubleMatrix> GRAD = new Function<DoubleArray, DoubleMatrix>() {

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleMatrix apply(final DoubleArray a) {
      final int n = X.size();
      final int m = a.size();
      final double[][] res = new double[n][m];
      for (int i = 0; i < n; i++) {
        final DoubleArray temp = PARAM_GRAD.evaluate(X.get(i), a);
        ArgChecker.isTrue(m == temp.size());
        for (int j = 0; j < m; j++) {
          res[i][j] = temp.get(j);
        }
      }
      return DoubleMatrix.copyOf(res);
    }
  };

  static {
    X = DoubleArray.of(20, i -> -Math.PI + i * Math.PI / 10);
    Y = DoubleArray.of(20, i -> TARGET.apply(X.get(i)));
    SIGMA = DoubleArray.of(20, i -> 0.1 * Math.exp(Math.abs(X.get(i)) / Math.PI));
    LS = new NonLinearLeastSquare();
  }

  public void solveExactTest() {
    final DoubleArray start = DoubleArray.of(1.2, 0.8, -0.2, -0.3);
    LeastSquareResults result = LS.solve(X, Y, SIGMA, PARAM_FUNCTION, PARAM_GRAD, start);
    assertEquals(0.0, result.getChiSq(), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(0), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(1), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(2), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(3), 1e-8);
    result = LS.solve(X, Y, SIGMA.get(0), PARAM_FUNCTION, PARAM_GRAD, start);
    assertEquals(0.0, result.getChiSq(), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(0), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(1), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(2), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(3), 1e-8);
    result = LS.solve(X, Y, PARAM_FUNCTION, PARAM_GRAD, start);
    assertEquals(0.0, result.getChiSq(), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(0), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(1), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(2), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(3), 1e-8);
  }

  public void solveExactTest2() {
    final DoubleArray start = DoubleArray.of(0.2, 1.8, 0.2, 0.3);
    final LeastSquareResults result = LS.solve(Y, SIGMA, FUNCTION, start);
    assertEquals(0.0, result.getChiSq(), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(0), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(1), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(2), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(3), 1e-8);
  }

  public void solveExactWithoutGradientTest() {

    final DoubleArray start = DoubleArray.of(1.2, 0.8, -0.2, -0.3);

    final NonLinearLeastSquare ls = new NonLinearLeastSquare();
    final LeastSquareResults result = ls.solve(X, Y, SIGMA, PARAM_FUNCTION, start);
    assertEquals(0.0, result.getChiSq(), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(0), 1e-8);
    assertEquals(1.0, result.getFitParameters().get(1), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(2), 1e-8);
    assertEquals(0.0, result.getFitParameters().get(3), 1e-8);
  }

  public void solveRandomNoiseTest() {
    final MatrixAlgebra ma = new OGMatrixAlgebra();
    final double[] y = new double[20];
    for (int i = 0; i < 20; i++) {
      y[i] = Y.get(i) + SIGMA.get(i) * NORMAL.nextRandom();
    }
    final DoubleArray start = DoubleArray.of(0.7, 1.4, 0.2, -0.3);
    final NonLinearLeastSquare ls = new NonLinearLeastSquare();
    final LeastSquareResults res = ls.solve(X, DoubleArray.copyOf(y), SIGMA, PARAM_FUNCTION, PARAM_GRAD, start);

    final double chiSqDoF = res.getChiSq() / 16;
    assertTrue(chiSqDoF > 0.25);
    assertTrue(chiSqDoF < 3.0);

    final DoubleArray trueValues = DoubleArray.of(1, 1, 0, 0);
    final DoubleArray delta = (DoubleArray) ma.subtract(res.getFitParameters(), trueValues);

    final LUDecompositionCommons decmp = new LUDecompositionCommons();
    final LUDecompositionResult decmpRes = decmp.apply(res.getCovariance());
    final DoubleMatrix invCovariance = decmpRes.solve(DoubleMatrix.identity(4));

    double z = ma.getInnerProduct(delta, ma.multiply(invCovariance, delta));
    z = Math.sqrt(z);

    assertTrue(z < 3.0);

  }

  public void smallPertubationTest() {
    final MatrixAlgebra ma = new OGMatrixAlgebra();
    final double[] dy = new double[20];
    for (int i = 0; i < 20; i++) {
      dy[i] = 0.1 * SIGMA.get(i) * NORMAL.nextRandom();
    }
    final DoubleArray deltaY = DoubleArray.copyOf(dy);
    final DoubleArray solution = DoubleArray.of(1.0, 1.0, 0.0, 0.0);
    final NonLinearLeastSquare ls = new NonLinearLeastSquare();
    final DoubleMatrix res = ls.calInverseJacobian(SIGMA, FUNCTION, GRAD, solution);

    final DoubleArray deltaParms = (DoubleArray) ma.multiply(res, deltaY);

    final DoubleArray y = (DoubleArray) ma.add(Y, deltaY);

    final LeastSquareResults lsRes = ls.solve(X, y, SIGMA, PARAM_FUNCTION, PARAM_GRAD, solution);
    final DoubleArray trueDeltaParms = (DoubleArray) ma.subtract(lsRes.getFitParameters(), solution);

    assertEquals(trueDeltaParms.get(0), deltaParms.get(0), 5e-5);
    assertEquals(trueDeltaParms.get(1), deltaParms.get(1), 5e-5);
    assertEquals(trueDeltaParms.get(2), deltaParms.get(2), 5e-5);
    assertEquals(trueDeltaParms.get(3), deltaParms.get(3), 5e-5);
  }

}
