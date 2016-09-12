/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;

/**
 * Test {@link SabrHaganVolatilityFunctionProvider}.
 */
@Test
public class SabrHaganVolatilityFunctionProviderTest extends SabrVolatilityFunctionProviderTestCase {

  private static ProbabilityDistribution<Double> NORMAL =
      new NormalDistribution(0d, 1d, new MersenneTwister64(MersenneTwister.DEFAULT_SEED));
  private static SabrHaganVolatilityFunctionProvider FUNCTION = SabrHaganVolatilityFunctionProvider.DEFAULT;

  private static final double ALPHA = 0.05;
  private static final double BETA = 0.50;
  private static final double RHO = -0.25;
  private static final double NU = 0.4;
  private static final double F = 0.05;
  private static final SabrFormulaData DATA = SabrFormulaData.of(ALPHA, BETA, RHO, NU);
  private static final double T = 4.5;
  private static final double STRIKE_ITM = 0.0450;
  private static final double STRIKE_OTM = 0.0550;

  private static EuropeanVanillaOption CALL_ATM = EuropeanVanillaOption.of(F, T, CALL);
  private static EuropeanVanillaOption CALL_ITM = EuropeanVanillaOption.of(STRIKE_ITM, T, CALL);
  private static EuropeanVanillaOption CALL_OTM = EuropeanVanillaOption.of(STRIKE_OTM, T, CALL);
  
  private static final double TOLERANCE_VOL_LIMIT = 1.0E-5;

  @Override
  protected VolatilityFunctionProvider<SabrFormulaData> getFunction() {
    return FUNCTION;
  }

  @Test
  /**
   * Test if the Hagan volatility function implementation around ATM is numerically stable enough (the finite difference slope should be small enough).
   */
  public void testATMSmoothness() {
    double timeToExpiry = 1;
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
      SabrFormulaData SabrData = SabrFormulaData.of(alpha, beta, rho, nu);
      sabrVolatilty[looppts + nbPoints] =
          FUNCTION.volatility(forward, strike[looppts + nbPoints], timeToExpiry, SabrData);
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
    SabrFormulaData data = DATA.withAlpha(0.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    double volatility = FUNCTION.volatility(F, STRIKE_ITM, T, data);
    ValueDerivatives volatilityAdjoint = FUNCTION.volatilityAdjoint(F, STRIKE_ITM, T, data);
    assertEquals(volatility, volatilityAdjoint.getValue(), tol);
    assertEquals(0.0, volatilityAdjoint.getDerivative(0), tol);
    assertEquals(0.0, volatilityAdjoint.getDerivative(1), tol);
    assertEquals(1e7, volatilityAdjoint.getDerivative(2), tol);
    assertEquals(0.0, volatilityAdjoint.getDerivative(3), tol);
    assertEquals(0.0, volatilityAdjoint.getDerivative(4), tol);
    assertEquals(0.0, volatilityAdjoint.getDerivative(5), tol);
  }

  @Test
  public void testVolatilityAdjointSmallAlpha() {
    double eps = 1e-7;
    double tol = 1e-3;
    SabrFormulaData data = DATA.withAlpha(1e-5);
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
    SabrFormulaData data = DATA.withBeta(0.0);
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
    SabrFormulaData data = DATA.withBeta(1.0);
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
    SabrFormulaData data = DATA.withNu(0.0);
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
    SabrFormulaData data = DATA.withRho(-1.0);
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
    SabrFormulaData data = DATA.withRho(1.0);
    testVolatilityAdjoint(F, CALL_ATM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
    testVolatilityAdjoint(F, CALL_OTM, data, eps, tol);
  }

  @Test
  public void testVolatilityAdjointLargeRhoZLessThan1() {
    double eps = 1e-4;
    double tol = 1e-5;
    SabrFormulaData data = DATA.withRho(1.0 - 1e-9);
    testVolatilityAdjoint(F, CALL_ITM, data, eps, tol);
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
      SabrFormulaData data = SabrFormulaData.of(alpha, beta, rho, nu);
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
    SabrFormulaData sabrData = SabrFormulaData.of(alpha, beta, rho, nu);
    ValueDerivatives adj = FUNCTION.volatilityAdjoint(f, k, t, sabrData);
    double bsDelta = BlackFormulaRepository.delta(f, k, t, adj.getValue(), true);
    double bsVega = BlackFormulaRepository.vega(f, k, t, adj.getValue());
    double volForwardSense = adj.getDerivative(1);
    double delta = bsDelta + bsVega * volForwardSense;

    SabrFormulaData data = SabrFormulaData.of(alpha, beta, rho, nu);
    double volUp = FUNCTION.volatility(f + eps, k, t, data);
    double volDown = FUNCTION.volatility(f - eps, k, t, data);
    double priceUp = BlackFormulaRepository.price(f + eps, k, t, volUp, true);
    double price = BlackFormulaRepository.price(f, k, t, adj.getValue(), true);
    double priceDown = BlackFormulaRepository.price(f - eps, k, t, volDown, true);
    double fdDelta = (priceUp - priceDown) / 2 / eps;
    assertEquals(fdDelta, delta, 1e-6);

    double bsVanna = BlackFormulaRepository.vanna(f, k, t, adj.getValue());
    double bsGamma = BlackFormulaRepository.gamma(f, k, t, adj.getValue());

    double[] volD1 = new double[5];
    double[][] volD2 = new double[2][2];
    FUNCTION.volatilityAdjoint2(f, k, t, sabrData, volD1, volD2);
    double d2Sigmad2Fwd = volD2[0][0];
    double gamma = bsGamma + 2 * bsVanna * adj.getDerivative(1) + bsVega * d2Sigmad2Fwd;
    double fdGamma = (priceUp + priceDown - 2 * price) / eps / eps;

    double d2Sigmad2FwdFD = (volUp + volDown - 2 * adj.getValue()) / eps / eps;
    assertEquals(d2Sigmad2FwdFD, d2Sigmad2Fwd, 1e-4);

    assertEquals(fdGamma, gamma, 1e-2);
  }

  /**
   * Check that $\rho \simeq 1$ case is smoothly connected with a general case, i.e., 
   * comparing the approximated computation and full computation around the cutoff, which is currently $\rho = 1.0 - 1.0e-5$
   */
  @Test
  public void test_rho_close_to_1() {
    double rhoEps = 1.e-5;
    // rhoIn is larger than the cutoff, 
    // thus vol and sensitivities are computed by approximation formulas which are regular in the limit rho -> 1.
    double rhoIn = 1.0 - 0.5 * rhoEps;
    double rho1 = 1.0d;
    // rhoOut is smaller than the cutoff, thus vol and sensitivities are computed by full formula.
    double rhoOut = 1.0 - 1.5 * rhoEps;
    SabrFormulaData data1 = SabrFormulaData.of(ALPHA, BETA, rho1, NU);
    SabrFormulaData dataIn = SabrFormulaData.of(ALPHA, BETA, rhoIn, NU);
    SabrFormulaData dataOut = SabrFormulaData.of(ALPHA, BETA, rhoOut, NU);

    /*
     * z<1 case, i.e., finite values in the rho->1 limit
     */
    double volatilityOut = FUNCTION.volatility(F, STRIKE_OTM, T, dataOut);
    double[] adjointOut = toArray(FUNCTION.volatilityAdjoint(F, STRIKE_OTM, T, dataOut));
    double[] volatilityDOut = new double[6];
    double[][] volatilityD2Out = new double[2][2];
    double volatility2Out = FUNCTION.volatilityAdjoint2(F, STRIKE_OTM, T, dataOut, volatilityDOut, volatilityD2Out);

    double volatilityIn = FUNCTION.volatility(F, STRIKE_OTM, T, dataIn);
    double[] adjointIn = toArray(FUNCTION.volatilityAdjoint(F, STRIKE_OTM, T, dataIn));
    double[] volatilityDIn = new double[6];
    double[][] volatilityD2In = new double[2][2];
    double volatility2In = FUNCTION.volatilityAdjoint2(F, STRIKE_OTM, T, dataIn, volatilityDIn, volatilityD2In);
    
    double volatility1 = FUNCTION.volatility(F, STRIKE_OTM, T, data1);

    assertEquals(volatilityOut, volatility1, TOLERANCE_VOL_LIMIT);
    assertEquals(volatilityOut, volatilityIn, TOLERANCE_VOL_LIMIT);
    assertEquals(volatility2Out, volatility2In, TOLERANCE_VOL_LIMIT);
    for (int i = 0; i < adjointOut.length; ++i) {
      double ref = adjointOut[i];
      assertEquals(adjointOut[i], adjointIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-3);
    }
    for (int i = 0; i < volatilityDOut.length; ++i) {
      double ref = volatilityDOut[i];
      assertEquals(volatilityDOut[i], volatilityDIn[i], Math.max(Math.abs(ref), 1.0) * 1.e-3);
    }
    
  }
  
  /**
   * The limit for $rhp \to 1$ when $z \geq 1$ does not exists. Returning an illegal argument.
   */
  @Test
  public void test_rho_close_to_1_large() {
    double rhoEps = 1.e-5;
    double rhoIn = 1.0 - 0.5 * rhoEps;
    SabrFormulaData dataIn = SabrFormulaData.of(ALPHA, BETA, rhoIn, NU);
    assertThrowsIllegalArg(() -> FUNCTION.volatility(10 * F, STRIKE_ITM, T, dataIn));
    assertThrowsIllegalArg(() -> FUNCTION.volatilityAdjoint(10 * F, STRIKE_ITM, T, dataIn));
  }

  public void coverage() {
    coverImmutableBean(FUNCTION);
  }

  public void test_serialization() {
    assertSerialization(FUNCTION);
  }

  //-------------------------------------------------------------------------
  private enum SabrParameter {
    Forward, Strike, Alpha, Beta, Nu, Rho
  }

  private void testVolatilityAdjoint(double forward, EuropeanVanillaOption optionData, SabrFormulaData sabrData,
      double eps, double tol) {
    double volatility = FUNCTION.volatility(forward, optionData.getStrike(), optionData.getTimeToExpiry(), sabrData);
    double[] volatilityAdjoint = toArray(FUNCTION.volatilityAdjoint(
        forward, optionData.getStrike(), optionData.getTimeToExpiry(), sabrData));
    assertEquals(volatility, volatilityAdjoint[0], tol);
    assertEqualsRelTol("Forward Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Forward, eps), volatilityAdjoint[1], tol);
    assertEqualsRelTol("Strike Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Strike, eps), volatilityAdjoint[2], tol);
    assertEqualsRelTol("Alpha Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Alpha, eps), volatilityAdjoint[3], tol);
    assertEqualsRelTol("Beta Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Beta, eps), volatilityAdjoint[4], tol);
    assertEqualsRelTol("Rho Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Rho, eps), volatilityAdjoint[5], tol);
    assertEqualsRelTol("Nu Sensitivity" + sabrData.toString(),
        fdSensitivity(optionData, forward, sabrData, SabrParameter.Nu, eps), volatilityAdjoint[6], tol);
  }

  private double[] toArray(ValueDerivatives valueDerivatives) {
    double[] derivatives = valueDerivatives.getDerivatives().toArray();
    double[] res = new double[derivatives.length + 1];
    res[0] = valueDerivatives.getValue();
    System.arraycopy(derivatives, 0, res, 1, derivatives.length);
    return res;
  }

  private void volatilityAdjoint2ForInstrument(EuropeanVanillaOption option, double tolerance1,
      double tolerance2) {
    // vol
    double volatility = FUNCTION.volatility(F, option.getStrike(), option.getTimeToExpiry(), DATA);
    double[] volatilityAdjoint = toArray(FUNCTION.volatilityAdjoint(F, option.getStrike(), option.getTimeToExpiry(), DATA));
    double[] volD = new double[6];
    double[][] volD2 = new double[2][2];
    double vol = FUNCTION.volatilityAdjoint2(F, option.getStrike(), option.getTimeToExpiry(), DATA, volD, volD2);
    assertEquals(volatility, vol, tolerance1);
    // Derivative
    for (int loopder = 0; loopder < 6; loopder++) {
      assertEquals(volatilityAdjoint[loopder + 1], volD[loopder], tolerance1);
    }
    // Derivative forward-forward
    double deltaF = 0.000001;
    double volatilityFP = FUNCTION.volatility(F + deltaF, option.getStrike(), option.getTimeToExpiry(), DATA);
    double volatilityFM = FUNCTION.volatility(F - deltaF, option.getStrike(), option.getTimeToExpiry(), DATA);
    double derivativeFF_FD = (volatilityFP + volatilityFM - 2 * volatility) / (deltaF * deltaF);
    assertEquals(derivativeFF_FD, volD2[0][0], tolerance2);
    // Derivative strike-strike
    double deltaK = 0.000001;
    double volatilityKP = FUNCTION.volatility(F, option.getStrike() + deltaK, option.getTimeToExpiry(), DATA);
    double volatilityKM = FUNCTION.volatility(F, option.getStrike() - deltaK, option.getTimeToExpiry(), DATA);
    double derivativeKK_FD = (volatilityKP + volatilityKM - 2 * volatility) / (deltaK * deltaK);
    assertEquals(derivativeKK_FD, volD2[1][1], tolerance2);
    // Derivative strike-forward
    double volatilityFPKP = FUNCTION.volatility(F + deltaF, option.getStrike() + deltaK, option.getTimeToExpiry(), DATA);
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
      SabrFormulaData sabrData, SabrParameter param, double delta) {

    Function<SabrFormulaData, Double> funcC = null;
    Function<SabrFormulaData, Double> funcB = null;
    Function<SabrFormulaData, Double> funcA = null;
    SabrFormulaData dataC = null;
    SabrFormulaData dataB = sabrData;
    SabrFormulaData dataA = null;
    Function<SabrFormulaData, Double> func = getVolatilityFunction(optionData, forward);

    FiniteDifferenceType fdType = null;

    switch (param) {
      case Strike:
        double strike = optionData.getStrike();
        if (strike >= delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          funcA = getVolatilityFunction(withStrike(optionData, strike - delta), forward);
          funcC = getVolatilityFunction(withStrike(optionData, strike + delta), forward);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          funcA = func;
          funcB = getVolatilityFunction(withStrike(optionData, strike + delta), forward);
          funcC = getVolatilityFunction(withStrike(optionData, strike + 2 * delta), forward);
        }
        dataC = sabrData;
        dataB = sabrData;
        dataA = sabrData;
        break;
      case Forward:
        if (forward > delta) {
          fdType = FiniteDifferenceType.CENTRAL;
          funcA = getVolatilityFunction(optionData, forward - delta);
          funcC = getVolatilityFunction(optionData, forward + delta);
        } else {
          fdType = FiniteDifferenceType.FORWARD;
          funcA = func;
          funcB = getVolatilityFunction(optionData, forward + delta);
          funcC = getVolatilityFunction(optionData, forward + 2 * delta);
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
          return (-1.5 * funcA.apply(dataA) + 2.0 * funcB.apply(dataB) - 0.5 * funcC.apply(dataC)) / delta;
        case BACKWARD:
          return (0.5 * funcA.apply(dataA) - 2.0 * funcB.apply(dataB) + 1.5 * funcC.apply(dataC)) / delta;
        case CENTRAL:
          return (funcC.apply(dataC) - funcA.apply(dataA)) / 2.0 / delta;
        default:
          throw new MathException("enum not found");
      }
    }
    throw new MathException("enum not found");
  }

  private Function<SabrFormulaData, Double> getVolatilityFunction(EuropeanVanillaOption option, double forward) {
    return new Function<SabrFormulaData, Double>() {
      @Override
      public Double apply(SabrFormulaData data) {
        ArgChecker.notNull(data, "data");
        return FUNCTION.volatility(forward, option.getStrike(), option.getTimeToExpiry(), data);
      }
    };
  }

  private EuropeanVanillaOption withStrike(EuropeanVanillaOption option, double strike) {
    return EuropeanVanillaOption.of(strike, option.getTimeToExpiry(), option.getPutCall());
  }

}
