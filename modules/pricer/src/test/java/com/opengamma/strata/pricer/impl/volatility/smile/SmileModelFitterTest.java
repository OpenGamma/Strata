/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.cern.MersenneTwister;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;

/**
 * Test case for smile model fitters.
 * 
 * @param <T> the smile model data
 */
public abstract class SmileModelFitterTest<T extends SmileModelData> {
  // CSOFF: VisibilityModifier

  protected static double TIME_TO_EXPIRY = 7.0;
  protected static double F = 0.03;
  private static RandomEngine UNIFORM = new MersenneTwister();
  protected static double[] STRIKES = new double[] {0.005, 0.01, 0.02, 0.03, 0.04, 0.05, 0.07, 0.1};

  protected double[] _cleanVols;
  protected double[] _noisyVols;
  protected double[] _errors;
  protected VolatilityFunctionProvider<T> _model;
  protected SmileModelFitter<T> _fitter;
  protected SmileModelFitter<T> _nosiyFitter;
  protected double _chiSqEps = 1e-6;
  protected double _paramValueEps = 1e-6;

  abstract Logger getlogger();

  abstract VolatilityFunctionProvider<T> getModel();

  abstract T getModelData();

  abstract SmileModelFitter<T> getFitter(
      double forward,
      double[] strikes,
      double timeToExpiry,
      double[] impliedVols,
      double[] error,
      VolatilityFunctionProvider<T> model);

  abstract double[][] getStartValues();

  abstract double[] getRandomStartValues();

  abstract BitSet[] getFixedValues();

  SmileModelFitterTest() {
    VolatilityFunctionProvider<T> model = getModel();
    T data = getModelData();
    int n = STRIKES.length;
    _noisyVols = new double[n];
    _errors = new double[n];
    _cleanVols = new double[n];
    Arrays.fill(_errors, 1e-4);
    for (int i = 0; i < n; i++) {
      _cleanVols[i] = model.volatility(F, STRIKES[i], TIME_TO_EXPIRY, data);
      _noisyVols[i] = _cleanVols[i] + UNIFORM.nextDouble() * _errors[i];
    }
    _fitter = getFitter(F, STRIKES, TIME_TO_EXPIRY, _cleanVols, _errors, model);
    _nosiyFitter = getFitter(F, STRIKES, TIME_TO_EXPIRY, _noisyVols, _errors, model);
  }

  @Test
  @SuppressWarnings("unused")
  public void testExactFit() {
    double[][] start = getStartValues();
    BitSet[] fixed = getFixedValues();
    int nStartPoints = start.length;
    ArgChecker.isTrue(fixed.length == nStartPoints);
    for (int trys = 0; trys < nStartPoints; trys++) {
      LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start[trys]), fixed[trys]);
      DoubleArray res = results.getModelParameters();
      assertThat(0.0).isCloseTo(results.getChiSq(), offset(_chiSqEps));
      int n = res.size();
      T data = getModelData();
      assertThat(data.getNumberOfParameters()).isEqualTo(n);
      for (int i = 0; i < n; i++) {
        assertThat(data.getParameter(i)).isCloseTo(res.get(i), offset(_paramValueEps));
      }
    }
  }

  @Test
  public void testNoisyFit() {
    double[][] start = getStartValues();
    BitSet[] fixed = getFixedValues();
    int nStartPoints = start.length;
    ArgChecker.isTrue(fixed.length == nStartPoints);
    for (int trys = 0; trys < nStartPoints; trys++) {
      LeastSquareResultsWithTransform results = _nosiyFitter.solve(DoubleArray.copyOf(start[trys]), fixed[trys]);
      DoubleArray res = results.getModelParameters();
      double eps = 1e-2;
      assertThat(results.getChiSq() < 7).isTrue();
      int n = res.size();
      T data = getModelData();
      assertThat(data.getNumberOfParameters()).isEqualTo(n);
      for (int i = 0; i < n; i++) {
        assertThat(data.getParameter(i)).isCloseTo(res.get(i), offset(eps));
      }
    }
  }

  @Test
  public void timeTest() {
    long start = 0;
    int hotspotWarmupCycles = 200;
    int benchmarkCycles = 1000;
    int nStarts = getStartValues().length;
    for (int i = 0; i < hotspotWarmupCycles; i++) {
      testNoisyFit();
    }
    start = System.nanoTime();
    for (int i = 0; i < benchmarkCycles; i++) {
      testNoisyFit();
    }
    long time = System.nanoTime() - start;
    getlogger().info("time per fit: " + ((double) time) / benchmarkCycles / nStarts + "ms");
  }

  @Test
  public void horribleMarketDataTest() {
    double forward = 0.0059875;
    double[] strikes = new double[] {0.0012499999999999734, 0.0024999999999999467, 0.003750000000000031,
        0.0050000000000000044, 0.006249999999999978, 0.007499999999999951, 0.008750000000000036,
        0.010000000000000009, 0.011249999999999982, 0.012499999999999956, 0.01375000000000004, 0.015000000000000013,
        0.016249999999999987, 0.01749999999999996, 0.018750000000000044,
        0.020000000000000018, 0.02124999999999999, 0.022499999999999964, 0.02375000000000005, 0.025000000000000022,
        0.026249999999999996, 0.02749999999999997, 0.028750000000000053,
        0.030000000000000027};
    double expiry = 0.09041095890410959;
    double[] vols = new double[] {2.7100433855959642, 1.5506135190088546, 0.9083977239618538, 0.738416513934868,
        0.8806973450124451, 1.0906290439592792, 1.2461975189027226, 1.496275983572826,
        1.5885915338673156, 1.4842142974195722, 1.7667347426399058, 1.4550288621444052, 1.0651798188736166,
        1.143318270172714, 1.216215092528441, 1.2845258218014657, 1.3488224665755535,
        1.9259326343836376, 1.9868728791190922, 2.0441767092857317, 2.0982583238541026, 2.1494622372820675,
        2.198020785622251, 2.244237863291375};
    int n = strikes.length;
    double[] errors = new double[n];
    Arrays.fill(errors, 0.01); //1% error
    SmileModelFitter<T> fitter = getFitter(forward, strikes, expiry, vols, errors, getModel());
    LeastSquareResults best = null;
    BitSet fixed = new BitSet();
    for (int i = 0; i < 5; i++) {
      double[] start = getRandomStartValues();

      //   int nStartPoints = start.length;
      LeastSquareResults lsRes = fitter.solve(DoubleArray.copyOf(start), fixed);
      if (best == null) {
        best = lsRes;
      } else {
        if (lsRes.getChiSq() < best.getChiSq()) {
          best = lsRes;
        }
      }
    }
    if (best != null) {
      assertThat(best.getChiSq() < 24000).isTrue(); //average error 31.6% - not a good fit, but the data is horrible
    }
  }

  @Test
  public void testJacobian() {
    T data = getModelData();
    int n = data.getNumberOfParameters();
    double[] temp = new double[n];
    for (int i = 0; i < n; i++) {
      temp[i] = data.getParameter(i);
    }
    DoubleArray x = DoubleArray.copyOf(temp);

    testJacobian(x);
  }

  // random test to be turned off
  @Disabled
  public void testRandomJacobian() {
    for (int i = 0; i < 10; i++) {
      double[] temp = getRandomStartValues();
      DoubleArray x = DoubleArray.copyOf(temp);
      try {
        testJacobian(x);
      } catch (AssertionError e) {
        System.out.println("Jacobian test failed at " + x.toString());
        throw e;
      }
    }
  }

  private void testJacobian(DoubleArray x) {
    int n = x.size();
    Function<DoubleArray, DoubleArray> func = _fitter.getModelValueFunction();
    Function<DoubleArray, DoubleMatrix> jacFunc = _fitter.getModelJacobianFunction();
    VectorFieldFirstOrderDifferentiator differ = new VectorFieldFirstOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix> jacFuncFD = differ.differentiate(func);
    DoubleMatrix jac = jacFunc.apply(x);
    DoubleMatrix jacFD = jacFuncFD.apply(x);
    int rows = jacFD.rowCount();
    int cols = jacFD.columnCount();

    assertThat(_cleanVols.length).isEqualTo(rows);
    assertThat(n).isEqualTo(cols);
    assertThat(rows).isEqualTo(jac.rowCount());
    assertThat(cols).isEqualTo(jac.columnCount());
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        assertThat(jacFD.get(i, j)).isCloseTo(jac.get(i, j), offset(2e-2));
      }
    }
  }

}
