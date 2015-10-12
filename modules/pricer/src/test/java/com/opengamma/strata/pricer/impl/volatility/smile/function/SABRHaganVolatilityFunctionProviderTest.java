/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;

import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.BisectionSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * Test {@link SABRHaganVolatilityFunctionProvider}.
 */
@Test
public class SABRHaganVolatilityFunctionProviderTest extends SABRVolatilityFunctionProviderTestCase {

  private static ProbabilityDistribution<Double> NORMAL =
      new NormalDistribution(0d, 1d, new MersenneTwister64(MersenneTwister.DEFAULT_SEED));
  private static SABRHaganVolatilityFunctionProvider FUNCTION = SABRHaganVolatilityFunctionProvider.DEFAULT;

  private static final double ALPHA = 0.05;
  private static final double BETA = 0.50;
  private static final double RHO = -0.25;
  private static final double NU = 0.4;
  private static final double F = 0.05;
  private static final SABRFormulaData DATA = SABRFormulaData.of(ALPHA, BETA, RHO, NU);
  private static final double T = 4.5;
  private static final double STRIKE_ITM = 0.0450;
  private static final double STRIKE_OTM = 0.0550;

  private static EuropeanVanillaOption CALL_ATM = EuropeanVanillaOption.of(F, T, CALL);
  private static EuropeanVanillaOption CALL_ITM = EuropeanVanillaOption.of(STRIKE_ITM, T, CALL);
  private static EuropeanVanillaOption CALL_OTM = EuropeanVanillaOption.of(STRIKE_OTM, T, CALL);

  @Override
  protected VolatilityFunctionProvider<SABRFormulaData> getFunction() {
    return FUNCTION;
  }

  @Test
  /**
   * Test if the Hagan volatility function implementation around ATM is numerically stable enough (the finite difference slope should be small enough).
   */
  public void testATMSmoothness() {
    double timeToExpiry = 1;
    EuropeanVanillaOption option;
    double alpha = 0.05;
    double beta = 0.5;
    double nu = 0.50;
    double rho = -0.25;
    int nbPoints = 100;
    double forward = 0.05;
    double[] sabrVolatilty = new double[2 * nbPoints + 1];
    double range = 5E-9;
    double strike[] = new double[2 * nbPoints + 1];
    for (int looppts = -nbPoints; looppts <= nbPoints; looppts++) {
      strike[looppts + nbPoints] = forward + ((double) looppts) / nbPoints * range;
      option = EuropeanVanillaOption.of(strike[looppts + nbPoints], timeToExpiry, CALL);
      SABRFormulaData SabrData = SABRFormulaData.of(alpha, beta, rho, nu);
      sabrVolatilty[looppts + nbPoints] = FUNCTION.getVolatilityFunction(option, forward).evaluate(SabrData);
    }
    for (int looppts = -nbPoints; looppts < nbPoints; looppts++) {
      assertTrue(Math.abs(sabrVolatilty[looppts + nbPoints + 1] - sabrVolatilty[looppts + nbPoints]) /
          (strike[looppts + nbPoints + 1] - strike[looppts + nbPoints]) < 20.0);
    }
  }

  @Test
  /**
   * Tests the first order adjoint derivatives for the SABR Hagan volatility function.
   * The derivatives with respect to the forward, strike, alpha, beta, rho and nu are provided.
   */
  public void testVolatilityAdjointDebug() {
    double eps = 1e-6;
    double tol = 1e-5;
    testVolatilityAdjoint(F, CALL_ATM, DATA, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, DATA, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, DATA, eps, tol);
  }

  /**
   * Test small strike edge case. Vol -> infinity as strike -> 0, so the strike is floored - tested against finite difference below this
   * floor will give spurious results
   */
  @Test
  public void testVolatilityAdjointSmallStrike() {
    double eps = 1e-10;
    double tol = 1e-6;
    double strike = 2e-6 * F;
    testVolatilityAdjoint(F, withStrike(CALL_ATM, strike), DATA, eps, tol);
  }

  /**
   *Test the alpha = 0 edge case. Implied vol is zero for alpha = 0, and except in the ATM case, the alpha sensitivity is infinite. We
   *choose to (arbitrarily) return 1e7 in this case.
   */
  @Test
  public void testVolatilityAdjointAlpha0() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withAlpha(0.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    double volatility = FUNCTION.getVolatilityFunction(CALL_ITM, F).evaluate(data);
    double[] volatilityAdjoint = FUNCTION.getVolatilityAdjoint(CALL_ITM, F, data);
    assertEquals(volatility, volatilityAdjoint[0], tol);
    assertEquals(0.0, volatilityAdjoint[1], tol);
    assertEquals(0.0, volatilityAdjoint[2], tol);
    assertEquals(1e7, volatilityAdjoint[3], tol);
    assertEquals(0.0, volatilityAdjoint[4], tol);
    assertEquals(0.0, volatilityAdjoint[5], tol);
    assertEquals(0.0, volatilityAdjoint[6], tol);
  }

  @Test
  public void testVolatilityAdjointSmallAlpha() {
    double eps = 1e-7;
    double tol = 1e-3;
    SABRFormulaData data = DATA.withAlpha(1e-5);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  /**
   *Test the beta = 0 edge case
   */
  @Test
  public void testVolatilityAdjointBeta0() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withBeta(0.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  /**
   *Test the beta = 1 edge case
   */
  @Test
  public void testVolatilityAdjointBeta1() {
    double eps = 1e-6;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withBeta(1.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  /**
   *Test the nu = 0 edge case
   */
  @Test
  public void testVolatilityAdjointNu0() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withNu(0.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, 2e-4);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, 5e-5);
  }

  /**
   *Test the rho = -1 edge case
   */
  @Test
  public void testVolatilityAdjointRhoM1() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withRho(-1.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  /**
   *Test the rho = 1 edge case
   */
  @Test
  public void testVolatilityAdjointRho1() {
    double eps = 1e-4;
    double tol = 1e-5;
    SABRFormulaData data = DATA.withRho(1.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityAdjointLargeRhoZLessThan1() {
    double eps = 1e-4;
    double tol = 1e-5;
    SABRFormulaData data = DATA.withRho(1.0 - 1e-9);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
  }

  @Test
  public void testVolatilityAdjointLargeRhoZGreaterThan1() {
    double eps = 1e-11;
    double tol = 1e-4;
    SABRFormulaData data = DATA.withRho(1.0 - 1e-9).withAlpha(0.15 * ALPHA);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
  }

  @Test
  public void testVolatilityModelAdjoint() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA;
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityModelAdjointRhoM1() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withRho(-1.0);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol); //z=0 case
    double z = -0.975;
    double strike = strikeForZ(z, F, ALPHA, BETA, NU);
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, eps, 5e-4);
    z = 2.0;
    strike = strikeForZ(z, F, ALPHA, BETA, NU);
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, eps, tol);
    z = -2.0;
    strike = strikeForZ(z, F, ALPHA, BETA, NU);
    //The true rho sensitivity at rho=-1 is infinity
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), DATA.withRho(-1 + 1e-3), eps, 1e-4);
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, 1e-6, 1.5);
  }

  @Test
  public void testVolatilityModelAdjointRhoP1() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withRho(1.0);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol); //z=0 case
    double z = 0.975;
    double strike = strikeForZ(z, F, ALPHA, BETA, NU);

    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, eps, 5e-2);
    z = -2.0;
    strike = strikeForZ(z, F, ALPHA, BETA, NU);
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, eps, 50 * tol);
    z = 2.0;
    strike = strikeForZ(z, F, ALPHA, BETA, NU);
    //The true rho sensitivity at rho= 1 is -infinity
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), DATA.withRho(1 - 1e-3), eps, 5e-5);
    testVolatilityModelAdjoint(F, withStrike(CALL_ATM, strike), data, 1e-6, 1.0);
  }

  @Test
  public void testVolatilityModelAdjointBeta0() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withBeta(0);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityModelAdjointBeta1() {
    double eps = 1e-5;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withBeta(1);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityModelAdjointNu0() {
    double eps = 1e-6;
    double tol = 1e-6;
    SABRFormulaData data = DATA.withNu(0);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityModelAdjoinAlpha0() {
    double eps = 1e-10;
    double tol = 1e-2;
    double z = getZ(F, CALL_ITM.getStrike(), ALPHA, BETA, NU);
    double alpha = z / 2e8;
    SABRFormulaData data = DATA.withAlpha(alpha);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
    z = getZ(F, CALL_OTM.getStrike(), ALPHA, BETA, NU);
    alpha = -z / 2e6;
    data = DATA.withAlpha(alpha);
    testVolatilityModelAdjoint(F, CALL_ITM, data, eps, tol);
  }

  /**
   *Test the beta = 0.0, rho = 1 edge case
   */
  @Test
  public void testVolatilityModelAdjointBeta0Rho1() {
    double eps = 1e-4;
    double tol = 1e-5;
    SABRFormulaData data = DATA.withRho(1.0).withBeta(0.0).withNu(20.0);
    testVolatilityModelAdjoint(F, CALL_ATM, data, eps, tol);
    // testVolatilityModelAdjoint(FORWARD, CALL_ITM, data, eps, tol);
    testVolatilityModelAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  /**
   * Tests the second order adjoint derivatives for the SABR Hagan volatility function. 
   * Only the derivatives with respect to the forward and the strike are provided.
   */
  public void volatilityAdjoint2() {
    volatilityAdjoint2ForInstrument(CALL_ITM, 1.0E-6, 1.0E-2);
    volatilityAdjoint2ForInstrument(CALL_ATM, 1.0E-6, 1.0E+2); // ATM the second order derivative is poor.
    volatilityAdjoint2ForInstrument(CALL_OTM, 1.0E-6, 1.0E-2);
  }

  //TODO write a fuzzer that hits SABR with random parameters
  @Test(enabled = false)
  public void testRandomParameters() {
    double eps = 1e-5;
    double tol = 1e-3;

    for (int count = 0; count < 100; count++) {
      double alpha = Math.exp(NORMAL.nextRandom() * 0.2 - 2);
      double beta = Math.random(); //TODO Uniform numbers in distribution
      double nu = Math.exp(NORMAL.nextRandom() * 0.3 - 1);
      double rho = 2 * Math.random() - 1;
      SABRFormulaData data = SABRFormulaData.of(alpha, beta, rho, nu);
      testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
      testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
      testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
    }
  }

  /**
   * Calculate the true SABR delta and gamma and compare with that found by finite difference
   */
  @Test(enabled = false)
  public void testGreeks() {
    double eps = 1e-3;
    double f = 1.2;
    double k = 1.4;
    double t = 5.0;
    double alpha = 0.3;
    double beta = 0.6;
    double rho = -0.4;
    double nu = 0.4;
    SABRFormulaData sabrData = SABRFormulaData.of(alpha, beta, rho, nu);

    double[] vol = FUNCTION.getVolatilityAdjoint(EuropeanVanillaOption.of(k, t, CALL), f, sabrData);
    double bsDelta = BlackFormulaRepository.delta(f, k, t, vol[0], true);
    double bsVega = BlackFormulaRepository.vega(f, k, t, vol[0]);
    double volForwardSense = vol[1];
    double delta = bsDelta + bsVega * volForwardSense;

    double volUp = FUNCTION.getVolatility(f + eps, k, t, alpha, beta, rho, nu);
    double volDown = FUNCTION.getVolatility(f - eps, k, t, alpha, beta, rho, nu);
    double priceUp = BlackFormulaRepository.price(f + eps, k, t, volUp, true);
    double price = BlackFormulaRepository.price(f, k, t, vol[0], true);
    double priceDown = BlackFormulaRepository.price(f - eps, k, t, volDown, true);
    double fdDelta = (priceUp - priceDown) / 2 / eps;
    assertEquals(fdDelta, delta, 1e-6);

    double bsVanna = BlackFormulaRepository.vanna(f, k, t, vol[0]);
    double bsGamma = BlackFormulaRepository.gamma(f, k, t, vol[0]);

    double[] volD1 = new double[5];
    double[][] volD2 = new double[2][2];
    FUNCTION.getVolatilityAdjoint2(EuropeanVanillaOption.of(k, t, CALL), f, sabrData, volD1, volD2);
    double d2Sigmad2Fwd = volD2[0][0];
    double gamma = bsGamma + 2 * bsVanna * vol[1] + bsVega * d2Sigmad2Fwd;
    double fdGamma = (priceUp + priceDown - 2 * price) / eps / eps;

    double d2Sigmad2FwdFD = (volUp + volDown - 2 * vol[0]) / eps / eps;
    assertEquals(d2Sigmad2FwdFD, d2Sigmad2Fwd, 1e-4);

    assertEquals(fdGamma, gamma, 1e-2);
  }

  /**
   * Check that $\rho \simeq 1$ case is smoothly connected with a general case, i.e., 
   * comparing the approximated computation and full computation around the cutoff, which is currently $\rho = 1.0 - 1.0e-5$
   * Note that the resulting numbers contain a large error if $\rho \simeq 1$ and $z \simeq 1$ are true at the same time
   */
  @Test
  public void largeRhoSmoothnessTest() {
    double rhoEps = 1.e-5;
    // rhoIn is larger than the cutoff, 
    // thus vol and sensitivities are computed by approximation formulas which are regular in the limit rho -> 1. 
    double rhoIn = 1.0 - 0.5 * rhoEps;
    // rhoOut is smaller than the cutoff, thus vol and sensitivities are computed by full formula. 
    double rhoOut = 1.0 - 1.5 * rhoEps;
    SABRFormulaData dataIn = SABRFormulaData.of(ALPHA, BETA, rhoIn, NU);
    SABRFormulaData dataOut = SABRFormulaData.of(ALPHA, BETA, rhoOut, NU);

    /*
     * z<1 case, i.e., finite values in the rho->1 limit
     */
    double volatilityOut = FUNCTION.getVolatility(CALL_OTM, F, dataOut);
    double[] adjointOut = FUNCTION.getVolatilityAdjoint(CALL_OTM, F, dataOut);
    double[] adjointModelOut = FUNCTION.getVolatilityModelAdjoint(CALL_OTM, F, dataOut);
    double[] volatilityDOut = new double[6];
    double[][] volatilityD2Out = new double[2][2];
    double volatility2Out = FUNCTION.getVolatilityAdjoint2(CALL_OTM, F, dataOut, volatilityDOut, volatilityD2Out);

    double volatilityIn = FUNCTION.getVolatility(CALL_OTM, F, dataIn);
    double[] adjointIn = FUNCTION.getVolatilityAdjoint(CALL_OTM, F, dataIn);
    double[] adjointModelIn = FUNCTION.getVolatilityModelAdjoint(CALL_OTM, F, dataIn);
    double[] volatilityDIn = new double[6];
    double[][] volatilityD2In = new double[2][2];
    double volatility2In = FUNCTION.getVolatilityAdjoint2(CALL_OTM, F, dataIn, volatilityDIn, volatilityD2In);

    assertEquals(volatilityOut, volatilityIn, rhoEps);
    assertEquals(volatility2Out, volatility2In, rhoEps);
    for (int i = 0; i < adjointOut.length; ++i) {
      double ref = adjointOut[i];
      assertEquals(adjointOut[i], adjointIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-3);
    }
    for (int i = 0; i < adjointModelOut.length; ++i) {
      double ref = adjointModelOut[i];
      assertEquals(adjointModelOut[i], adjointModelIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-3);
    }
    for (int i = 0; i < volatilityDOut.length; ++i) {
      double ref = volatilityDOut[i];
      assertEquals(volatilityDOut[i], volatilityDIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-3);
    }

    /*
     * z>1 case, runs into infinity or 0. 
     * Convergence speed is much faster (and typically smoother).
     */
    rhoIn = 1.0 - 0.999 * rhoEps;
    rhoOut = 1.0 - 1.001 * rhoEps;
    dataIn = SABRFormulaData.of(ALPHA, BETA, rhoIn, NU);
    dataOut = SABRFormulaData.of(ALPHA, BETA, rhoOut, NU);

    volatilityOut = FUNCTION.getVolatility(CALL_ITM, 3.0 * F, dataOut);
    adjointOut = FUNCTION.getVolatilityAdjoint(CALL_ITM, 3.0 * F, dataOut);
    adjointModelOut = FUNCTION.getVolatilityModelAdjoint(CALL_ITM, 3.0 * F, dataOut);
    volatilityDOut = new double[6];
    volatilityD2Out = new double[2][2];
    volatility2Out = FUNCTION.getVolatilityAdjoint2(CALL_ITM, 3.0 * F, dataOut, volatilityDOut, volatilityD2Out);

    volatilityIn = FUNCTION.getVolatility(CALL_ITM, 3.0 * F, dataIn);
    adjointIn = FUNCTION.getVolatilityAdjoint(CALL_ITM, 3.0 * F, dataIn);
    adjointModelIn = FUNCTION.getVolatilityModelAdjoint(CALL_ITM, 3.0 * F, dataIn);
    volatilityDIn = new double[6];
    volatilityD2In = new double[2][2];
    volatility2In = FUNCTION.getVolatilityAdjoint2(CALL_ITM, 3.0 * F, dataIn, volatilityDIn, volatilityD2In);

    assertEquals(volatilityOut, volatilityIn, rhoEps);
    assertEquals(volatility2Out, volatility2In, rhoEps);
    for (int i = 0; i < adjointOut.length; ++i) {
      double ref = adjointOut[i];
      assertEquals(ref, adjointIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-2);
    }
    for (int i = 0; i < adjointModelOut.length; ++i) {
      double ref = adjointModelOut[i];
      assertEquals(ref, adjointModelIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-2);
    }
    for (int i = 0; i < volatilityDOut.length; ++i) {
      double ref = volatilityDOut[i];
      assertEquals(ref, volatilityDIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-2);
    }
  }

  public void coverage() {
    coverImmutableBean(FUNCTION);
  }

  public void test_serialization() {
    assertSerialization(FUNCTION);
  }

  //-------------------------------------------------------------------------
  private double getZ(double forward, double strike, double alpha, double beta, double nu) {
    return nu / alpha * Math.pow(forward * strike, (1 - beta) / 2) * Math.log(forward / strike);
  }

  private double strikeForZ(double z, double forward, double alpha, double beta, double nu) {
    if (z == 0) {
      return forward;
    }
    if (beta == 1) {
      return forward * Math.exp(-alpha * z / nu);
    }
    BracketRoot bracketer = new BracketRoot();
    BisectionSingleRootFinder rootFinder = new BisectionSingleRootFinder(1e-5);
    Function1D<Double, Double> func = new Function1D<Double, Double>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public Double evaluate(Double strike) {
        return getZ(forward, strike, alpha, beta, nu) - z;
      }
    };
    double k = forward * Math.exp(-alpha * z / nu * Math.pow(forward, beta - 1));
    double l, h;
    if (z > 0) {
      h = k;
      l = h / 2;
    } else {
      l = k;
      h = 2 * l;
    }
    double[] brackets = bracketer.getBracketedPoints(func, l, h, forward / 20, 20 * forward);
    return rootFinder.getRoot(func, brackets[0], brackets[1]);
  }

  private enum SABRParameter {
    Forward, Strike, Alpha, Beta, Nu, Rho
  }

  private void testVolatilityAdjoint(double forward, EuropeanVanillaOption optionData, SABRFormulaData sabrData,
      double eps, double tol) {
    double volatility = FUNCTION.getVolatilityFunction(optionData, forward).evaluate(sabrData);
    double volatilityFromParameters = FUNCTION.getVolatility(forward, optionData.getStrike(),
        optionData.getTimeToExpiry(), sabrData.getAlpha(), sabrData.getBeta(), sabrData.getRho(), sabrData.getNu());
    double[] volatilityAdjoint = FUNCTION.getVolatilityAdjoint(optionData, forward, sabrData);
    double[] volatilityAdjointFromFunction =
        FUNCTION.getVolatilityAdjointFunction(optionData, forward).evaluate(sabrData);
    double[][] volatilityAdjointMultiple = FUNCTION.getVolatilityAdjointFunction(
        forward, new double[] {optionData.getStrike() }, optionData.getTimeToExpiry()).evaluate(sabrData);
    int nAdj = volatilityAdjoint.length;
    assertEquals(volatilityAdjointFromFunction.length, nAdj);
    assertEquals(volatilityAdjointMultiple[0].length, nAdj);
    for (int i = 0; i < nAdj; ++i) {
      assertEquals(volatilityAdjointFromFunction[i], volatilityAdjoint[i], tol);
      assertEquals(volatilityAdjointMultiple[0][i], volatilityAdjoint[i], tol);
    }
    assertEquals(volatility, volatilityAdjoint[0], tol);
    assertEquals(volatility, volatilityFromParameters, tol);
    assertEqualsRelTol("Forward Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Forward, eps), volatilityAdjoint[1], tol);
    assertEqualsRelTol("Strike Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Strike, eps), volatilityAdjoint[2], tol);
    assertEqualsRelTol("Alpha Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Alpha, eps), volatilityAdjoint[3], tol);
    assertEqualsRelTol("Beta Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Beta, eps), volatilityAdjoint[4], tol);
    assertEqualsRelTol("Rho Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Rho, eps), volatilityAdjoint[5], tol);
    assertEqualsRelTol("Nu Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Nu, eps), volatilityAdjoint[6], tol);
  }

  private void testVolatilityModelAdjoint(double forward, EuropeanVanillaOption optionData, SABRFormulaData sabrData,
      double eps, double tol) {
    double[] volatilityAdjoint = FUNCTION.getVolatilityModelAdjoint(optionData, forward, sabrData);
    double[] volatilityAdjointFromFunction = FUNCTION.getModelAdjointFunction(optionData, forward).evaluate(sabrData);
    double[][] volatilityAdjointMultiple = FUNCTION.getModelAdjointFunction(
        forward, new double[] {optionData.getStrike() }, optionData.getTimeToExpiry()).evaluate(sabrData);
    int nAdj = volatilityAdjoint.length;
    assertEquals(volatilityAdjointFromFunction.length, nAdj);
    assertEquals(volatilityAdjointMultiple[0].length, nAdj);
    for (int i = 0; i < nAdj; ++i) {
      assertEquals(volatilityAdjointFromFunction[i], volatilityAdjoint[i], tol);
      assertEquals(volatilityAdjointMultiple[0][i], volatilityAdjoint[i], tol);
    }
    assertEqualsRelTol("Alpha Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Alpha, eps), volatilityAdjoint[0], tol);
    assertEqualsRelTol("Beta Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Beta, eps), volatilityAdjoint[1], tol);
    assertEqualsRelTol("Rho Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Rho, eps), volatilityAdjoint[2], tol);
    assertEqualsRelTol("Nu Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SABRParameter.Nu, eps), volatilityAdjoint[3], tol);
  }

  private void volatilityAdjoint2ForInstrument(EuropeanVanillaOption option, double tolerance1,
      double tolerance2) {
    // Price
    double volatility = FUNCTION.getVolatilityFunction(option, F).evaluate(DATA);
    double[] volatilityAdjoint = FUNCTION.getVolatilityAdjoint(option, F, DATA);
    double[] volD = new double[6];
    double[][] volD2 = new double[2][2];
    double vol = FUNCTION.getVolatilityAdjoint2(option, F, DATA, volD, volD2);
    assertEquals(volatility, vol, tolerance1);
    // Derivative
    for (int loopder = 0; loopder < 6; loopder++) {
      assertEquals(volatilityAdjoint[loopder + 1], volD[loopder], tolerance1);
    }
    // Derivative forward-forward
    double deltaF = 0.000001;
    double volatilityFP = FUNCTION.getVolatilityFunction(option, F + deltaF).evaluate(DATA);
    double volatilityFM = FUNCTION.getVolatilityFunction(option, F - deltaF).evaluate(DATA);
    double derivativeFF_FD = (volatilityFP + volatilityFM - 2 * volatility) / (deltaF * deltaF);
    assertEquals(derivativeFF_FD, volD2[0][0], tolerance2);
    // Derivative strike-strike
    double deltaK = 0.000001;
    EuropeanVanillaOption optionKP = EuropeanVanillaOption.of(option.getStrike() + deltaK, T, CALL);
    EuropeanVanillaOption optionKM = EuropeanVanillaOption.of(option.getStrike() - deltaK, T, CALL);
    double volatilityKP = FUNCTION.getVolatilityFunction(optionKP, F).evaluate(DATA);
    double volatilityKM = FUNCTION.getVolatilityFunction(optionKM, F).evaluate(DATA);
    double derivativeKK_FD = (volatilityKP + volatilityKM - 2 * volatility) / (deltaK * deltaK);
    assertEquals(derivativeKK_FD, volD2[1][1], tolerance2);
    // Derivative strike-forward
    double volatilityFPKP = FUNCTION.getVolatilityFunction(optionKP, F + deltaF).evaluate(DATA);
    double derivativeFK_FD = (volatilityFPKP + volatility - volatilityFP - volatilityKP) / (deltaF * deltaK);
    assertEquals(derivativeFK_FD, volD2[0][1], tolerance2);
    assertEquals(volD2[0][1], volD2[1][0], 1E-6);
  }

  private void assertEqualsRelTol(String msg, double exp, double act, double tol) {
    double delta = (Math.abs(exp) + Math.abs(act)) * tol / 2.0;
    assertEquals(act, exp, delta, msg);
  }

  @SuppressWarnings("null")
  private double fdSensitivity(EuropeanVanillaOption optionData, double forward,
      SABRFormulaData sabrData, SABRParameter param, double delta) {

    Function1D<SABRFormulaData, Double> funcC = null;
    Function1D<SABRFormulaData, Double> funcB = null;
    Function1D<SABRFormulaData, Double> funcA = null;
    SABRFormulaData dataC = null;
    SABRFormulaData dataB = sabrData;
    SABRFormulaData dataA = null;
    Function1D<SABRFormulaData, Double> func = FUNCTION.getVolatilityFunction(optionData, forward);

    FiniteDifferenceType fdType = null;

    switch (param) {
      case Strike:
        double strike = optionData.getStrike();
        if (strike >= delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          funcA = FUNCTION.getVolatilityFunction(withStrike(optionData, strike - delta), forward);
          funcC = FUNCTION.getVolatilityFunction(withStrike(optionData, strike + delta), forward);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          funcA = func;
          funcB = FUNCTION.getVolatilityFunction(withStrike(optionData, strike + delta), forward);
          funcC = FUNCTION.getVolatilityFunction(withStrike(optionData, strike + 2 * delta), forward);
        }
        dataC = sabrData;
        dataB = sabrData;
        dataA = sabrData;
        break;
      case Forward:
        if (forward > delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          funcA = FUNCTION.getVolatilityFunction(optionData, forward - delta);
          funcC = FUNCTION.getVolatilityFunction(optionData, forward + delta);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          funcA = func;
          funcB = FUNCTION.getVolatilityFunction(optionData, forward + delta);
          funcC = FUNCTION.getVolatilityFunction(optionData, forward + 2 * delta);
        }
        dataC = sabrData;
        dataB = sabrData;
        dataA = sabrData;
        break;
      case Alpha:
        double a = sabrData.getAlpha();
        if (a >= delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          dataA = sabrData.withAlpha(a - delta);
          dataC = sabrData.withAlpha(a + delta);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          dataA = sabrData;
          dataB = sabrData.withAlpha(a + delta);
          dataC = sabrData.withAlpha(a + 2 * delta);
        }
        funcC = func;
        funcB = func;
        funcA = func;
        break;
      case Beta:
        double b = sabrData.getBeta();
        if (b >= delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          dataA = sabrData.withBeta(b - delta);
          dataC = sabrData.withBeta(b + delta);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          dataA = sabrData;
          dataB = sabrData.withBeta(b + delta);
          dataC = sabrData.withBeta(b + 2 * delta);
        }
        funcC = func;
        funcB = func;
        funcA = func;
        break;
      case Nu:
        double n = sabrData.getNu();
        if (n >= delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          dataA = sabrData.withNu(n - delta);
          dataC = sabrData.withNu(n + delta);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          dataA = sabrData;
          dataB = sabrData.withNu(n + delta);
          dataC = sabrData.withNu(n + 2 * delta);
        }
        funcC = func;
        funcB = func;
        funcA = func;
        break;
      case Rho:
        double r = sabrData.getRho();
        if ((r + 1) < delta) {
          fdType = FiniteDifferenceType.FORWARD;
          dataA = sabrData;
          dataB = sabrData.withRho(r + delta);
          dataC = sabrData.withRho(r + 2 * delta);
        } else if ((1 - r) < delta) {
          fdType = FiniteDifferenceType.BACKWARD;
          dataA = sabrData.withRho(r - 2 * delta);
          dataB = sabrData.withRho(r - delta);
          dataC = sabrData;
        } else {
          fdType = FiniteDifferenceType.CENTRAL;
          dataC = sabrData.withRho(r + delta);
          dataA = sabrData.withRho(r - delta);
        }
        funcC = func;
        funcB = func;
        funcA = func;
        break;
      default:
        throw new MathException("enum not found");
    }

    if (fdType != null) {
      switch (fdType) {
        case FORWARD:
          return (-1.5 * funcA.evaluate(dataA) + 2.0 * funcB.evaluate(dataB) - 0.5 * funcC.evaluate(dataC)) / delta;
        case BACKWARD:
          return (0.5 * funcA.evaluate(dataA) - 2.0 * funcB.evaluate(dataB) + 1.5 * funcC.evaluate(dataC)) / delta;
        case CENTRAL:
          return (funcC.evaluate(dataC) - funcA.evaluate(dataA)) / 2.0 / delta;
        default:
          throw new MathException("enum not found");
      }
    }
    throw new MathException("enum not found");
  }

  private EuropeanVanillaOption withStrike(EuropeanVanillaOption option, double strike) {
    return EuropeanVanillaOption.of(strike, option.getTimeToExpiry(), option.getPutCall());
  }
}
