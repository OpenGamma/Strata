/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.fitting;

import static org.testng.Assert.assertEquals;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * Test {@link SabrModelFitter}.
 */
@Test
public class SabrModelFitterTest extends SmileModelFitterTest<SabrFormulaData> {

  private static double ALPHA = 0.05;
  private static double BETA = 0.5;
  private static double RHO = -0.3;
  private static double NU = 0.2;
  private static Logger LOGGER = LoggerFactory.getLogger(SabrModelFitterTest.class);
  private static RandomEngine RANDOM = new MersenneTwister();

  SabrModelFitterTest() {
    _chiSqEps = 1e-4;
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
    return new double[][] { {0.1, 0.7, 0.0, 0.3 }, {0.01, 0.95, 0.9, 0.4 }, {0.01, 0.5, -0.7, 0.6 } };
  }

  @Override
  Logger getlogger() {
    return LOGGER;
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
    return new double[] {alpha, beta, rho, nu };
  }

  public void testExactFitOddStart() {
    double[] start = new double[] {0.01, 0.99, 0.9, 0.4 };
    LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start));
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

  public void testExactFitWithTransform() {
    double[] start = new double[] {0.01, 0.99, 0.9, 0.4 };
    NonLinearParameterTransforms transf = _fitter.getTransform(DoubleArray.copyOf(start));
    LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start), transf);
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

  public void testExactFitWithFixedBeta() {
    double[] start = new double[] {0.1, 0.5, 0.0, 0.3 };
    BitSet fixed = new BitSet();
    fixed.set(1);
    LeastSquareResultsWithTransform results = _fitter.solve(DoubleArray.copyOf(start), fixed);
    double[] res = results.getModelParameters().toArray();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

}
