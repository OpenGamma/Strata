/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.BitSet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.cern.MersenneTwister;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;

/**
 * Test {@link SabrModelFitter}.
 */
public class SabrModelFitterTest extends SmileModelFitterTest<SabrFormulaData> {

  private static double ALPHA = 0.05;
  private static double BETA = 0.5;
  private static double RHO = -0.3;
  private static double NU = 0.2;
  private static Logger log = LoggerFactory.getLogger(SabrModelFitterTest.class);
  private static RandomEngine RANDOM = new MersenneTwister();

  SabrModelFitterTest() {
    _chiSqEps = 1e-10;
  }

  @Override
  VolatilityFunctionProvider<SabrFormulaData> getModel() {
    return SabrHaganVolatilityFunctionProvider.DEFAULT;
  }

  @Override
  SabrFormulaData getModelData() {
    return SabrFormulaData.of(ALPHA, BETA, RHO, NU);
  }

  @Override
  SmileModelFitter<SabrFormulaData> getFitter(double forward, double[] strikes, double timeToExpiry,
      double[] impliedVols, double[] error, VolatilityFunctionProvider<SabrFormulaData> model) {
    return new SabrModelFitter(forward, DoubleArray.copyOf(strikes), timeToExpiry, DoubleArray.copyOf(impliedVols),
        DoubleArray.copyOf(error), model);
  }

  @Override
  double[][] getStartValues() {
    return new double[][] {{0.1, 0.7, 0.0, 0.3}, {0.01, 0.95, 0.9, 0.4}, {0.01, 0.5, -0.7, 0.6}};
  }

  @Override
  Logger getlogger() {
    return log;
  }

  @Override
  BitSet[] getFixedValues() {
    BitSet[] fixed = new BitSet[3];
    fixed[0] = new BitSet();
    fixed[1] = new BitSet();
    fixed[2] = new BitSet();
    fixed[2].set(1);
    return fixed;
  }

  @Override
  double[] getRandomStartValues() {
    double alpha = 0.1 + 0.4 * RANDOM.nextDouble();
    double beta = RANDOM.nextDouble();
    double rho = 2 * RANDOM.nextDouble() - 1;
    double nu = 1.5 * RANDOM.nextDouble();
    return new double[] {alpha, beta, rho, nu};
  }

  @Test
  public void testExactFitOddStart() {
    double[] start = new double[] {0.01, 0.99, 0.9, 0.4};
    LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start));
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertThat(ALPHA).isCloseTo(res[0], offset(eps));
    assertThat(BETA).isCloseTo(res[1], offset(eps));
    assertThat(RHO).isCloseTo(res[2], offset(eps));
    assertThat(NU).isCloseTo(res[3], offset(eps));
    assertThat(0.0).isCloseTo(results.getChiSq(), offset(eps));
  }

  @Test
  public void testExactFitWithTransform() {
    double[] start = new double[] {0.01, 0.99, 0.9, 0.4};
    NonLinearParameterTransforms transf = _fitter.getTransform(DoubleArray.copyOf(start));
    LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start), transf);
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertThat(ALPHA).isCloseTo(res[0], offset(eps));
    assertThat(BETA).isCloseTo(res[1], offset(eps));
    assertThat(RHO).isCloseTo(res[2], offset(eps));
    assertThat(NU).isCloseTo(res[3], offset(eps));
    assertThat(0.0).isCloseTo(results.getChiSq(), offset(eps));
  }

  @Test
  public void testExactFitWithFixedBeta() {
    DoubleArray start = DoubleArray.of(0.1, 0.5, 0.0, 0.3);
    BitSet fixed = new BitSet();
    fixed.set(1);
    LeastSquareResultsWithTransform results = _fitter.solve(start, fixed);
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertThat(ALPHA).isCloseTo(res[0], offset(eps));
    assertThat(BETA).isCloseTo(res[1], offset(eps));
    assertThat(RHO).isCloseTo(res[2], offset(eps));
    assertThat(NU).isCloseTo(res[3], offset(eps));
    assertThat(0.0).isCloseTo(results.getChiSq(), offset(eps));

    // sensitivity to data
    DoubleMatrix sensitivity = results.getModelParameterSensitivityToData();
    double shiftFd = 1.0E-5;
    for (int i = 0; i < _cleanVols.length; i++) {
      double[] volBumpedP = _cleanVols.clone();
      volBumpedP[i] += shiftFd;
      SabrModelFitter fitterP = new SabrModelFitter(F, DoubleArray.copyOf(STRIKES), TIME_TO_EXPIRY,
          DoubleArray.copyOf(volBumpedP), DoubleArray.copyOf(_errors), getModel());
      LeastSquareResultsWithTransform resultsBumpedP = fitterP.solve(start, fixed);
      DoubleArray parameterBumpedP = resultsBumpedP.getModelParameters();
      double[] volBumpedM = _cleanVols.clone();
      volBumpedM[i] -= shiftFd;
      SabrModelFitter fitterM = new SabrModelFitter(F, DoubleArray.copyOf(STRIKES), TIME_TO_EXPIRY,
          DoubleArray.copyOf(volBumpedM), DoubleArray.copyOf(_errors), getModel());
      LeastSquareResultsWithTransform resultsBumpedM = fitterM.solve(start, fixed);
      DoubleArray parameterBumpedM = resultsBumpedM.getModelParameters();
      DoubleArray sensitivityColumnFd = parameterBumpedP.minus(parameterBumpedM).dividedBy(2 * shiftFd);
      assertThat(sensitivityColumnFd.equalWithTolerance(sensitivity.column(i), 1.0E-6)).isTrue();
    }
  }

  @Test
  public void testNoisyFitWithFixedBeta() {
    DoubleArray start = DoubleArray.of(0.1, 0.5, 0.0, 0.3);
    BitSet fixed = new BitSet();
    fixed.set(1);
    LeastSquareResultsWithTransform results = _nosiyFitter.solve(start, fixed);
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-2;
    assertThat(ALPHA).isCloseTo(res[0], offset(eps));
    assertThat(BETA).isCloseTo(res[1], offset(eps));
    assertThat(RHO).isCloseTo(res[2], offset(eps));
    assertThat(NU).isCloseTo(res[3], offset(eps));
    assertThat(0.0).isCloseTo(results.getChiSq(), offset(10.0d));

    // sensitivity to data
    DoubleMatrix sensitivity = results.getModelParameterSensitivityToData();
    double shiftFd = 1.0E-5;
    for (int i = 0; i < _cleanVols.length; i++) {
      double[] volBumpedP = _noisyVols.clone();
      volBumpedP[i] += shiftFd;
      SabrModelFitter fitterP = new SabrModelFitter(F, DoubleArray.copyOf(STRIKES), TIME_TO_EXPIRY,
          DoubleArray.copyOf(volBumpedP), DoubleArray.copyOf(_errors), getModel());
      LeastSquareResultsWithTransform resultsBumpedP = fitterP.solve(start, fixed);
      DoubleArray parameterBumpedP = resultsBumpedP.getModelParameters();
      double[] volBumpedM = _noisyVols.clone();
      volBumpedM[i] -= shiftFd;
      SabrModelFitter fitterM = new SabrModelFitter(F, DoubleArray.copyOf(STRIKES), TIME_TO_EXPIRY,
          DoubleArray.copyOf(volBumpedM), DoubleArray.copyOf(_errors), getModel());
      LeastSquareResultsWithTransform resultsBumpedM = fitterM.solve(start, fixed);
      DoubleArray parameterBumpedM = resultsBumpedM.getModelParameters();
      DoubleArray sensitivityColumnFd = parameterBumpedP.minus(parameterBumpedM).dividedBy(2 * shiftFd);
      assertThat(sensitivityColumnFd.equalWithTolerance(sensitivity.column(i), 1.0E-2)).isTrue();
    }
  }

}
