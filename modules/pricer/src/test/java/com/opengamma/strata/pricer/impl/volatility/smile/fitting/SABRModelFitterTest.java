package com.opengamma.strata.pricer.impl.volatility.smile.fitting;

import static org.testng.Assert.assertEquals;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

@Test
public class SABRModelFitterTest extends SmileModelFitterTest<SABRFormulaData> {

  private static double ALPHA = 0.05;
  private static double BETA = 0.5;
  private static double RHO = -0.3;
  private static double NU = 0.2;
  private static Logger LOGGER = LoggerFactory.getLogger(SABRModelFitterTest.class);
  private static RandomEngine RANDOM = new MersenneTwister();

  public SABRModelFitterTest() {
    _chiSqEps = 1e-4;
  }

  @Override
  VolatilityFunctionProvider<SABRFormulaData> getModel() {
    return SABRHaganVolatilityFunctionProvider.DEFAULT;
  }

  @Override
  SABRFormulaData getModelData() {
    return SABRFormulaData.of(ALPHA, BETA, RHO, NU);
  }

  @Override
  SmileModelFitter<SABRFormulaData> getFitter(double forward, double[] strikes, double timeToExpiry,
      double[] impliedVols, double[] error, VolatilityFunctionProvider<SABRFormulaData> model) {
    return new SABRModelFitter(forward, strikes, timeToExpiry, impliedVols, error, model);
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
    LeastSquareResultsWithTransform results = _fitter.solve(new DoubleMatrix1D(start));
    double[] res = results.getModelParameters().getData();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

  public void testExactFitWithTransform() {
    double[] start = new double[] {0.01, 0.99, 0.9, 0.4 };
    NonLinearParameterTransforms transf = _fitter.getTransform(new DoubleMatrix1D(start));
    LeastSquareResultsWithTransform results = _fitter.solve(new DoubleMatrix1D(start), transf);
    double[] res = results.getModelParameters().getData();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

  @Test
  public void testExactFitWithFixedBeta() {
    double[] start = new double[] {0.1, 0.5, 0.0, 0.3 };
    BitSet fixed = new BitSet();
    fixed.set(1);
    LeastSquareResultsWithTransform results = _fitter.solve(new DoubleMatrix1D(start), fixed);
    double[] res = results.getModelParameters().getData();
    double eps = 1e-6;
    assertEquals(ALPHA, res[0], eps);
    assertEquals(BETA, res[1], eps);
    assertEquals(RHO, res[2], eps);
    assertEquals(NU, res[3], eps);
    assertEquals(0.0, results.getChiSq(), eps);
  }

}
