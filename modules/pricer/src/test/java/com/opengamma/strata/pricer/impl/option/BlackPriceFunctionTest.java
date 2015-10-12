/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * Test {@link BlackPriceFunction}.
 */
@Test
public class BlackPriceFunctionTest {

  private static final double T = 4.5;
  private static final double F = 104;
  private static final double DELTA = 10;
  private static final EuropeanVanillaOption ATM_CALL = EuropeanVanillaOption.of(F, T, CALL);
  private static final EuropeanVanillaOption ITM_CALL = EuropeanVanillaOption.of(F - DELTA, T, CALL);
  private static final EuropeanVanillaOption OTM_CALL = EuropeanVanillaOption.of(F + DELTA, T, CALL);
  private static final EuropeanVanillaOption CALL_0 = EuropeanVanillaOption.of(0.0, T, CALL);
  private static final EuropeanVanillaOption ITM_PUT = EuropeanVanillaOption.of(F + DELTA, T, PUT);
  private static final EuropeanVanillaOption OTM_PUT = EuropeanVanillaOption.of(F - DELTA, T, PUT);
  private static final double DF = 0.9;
  private static final double SIGMA = 0.5;
  private static final BlackFunctionData ATM_DATA = BlackFunctionData.of(F, DF, SIGMA);
  private static final BlackFunctionData ZERO_VOL_DATA = BlackFunctionData.of(F, DF, 0);
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final BlackPriceFunction FUNCTION = new BlackPriceFunction();

  public void testATMPrice() {
    final double sigmaRootT = ATM_DATA.getBlackVolatility() * Math.sqrt(ATM_CALL.getTimeToExpiry());
    assertEquals(DF * F * (2 * NORMAL.getCDF(sigmaRootT / 2) - 1), FUNCTION.getPriceFunction(ATM_CALL).evaluate(ATM_DATA), 1e-14);
  }

  public void testZeroVolPrice() {
    assertEquals(DF * DELTA, FUNCTION.getPriceFunction(ITM_CALL).evaluate(ZERO_VOL_DATA), 1e-15);
    assertEquals(0, FUNCTION.getPriceFunction(OTM_CALL).evaluate(ZERO_VOL_DATA), 1e-15);
    assertEquals(DF * DELTA, FUNCTION.getPriceFunction(ITM_PUT).evaluate(ZERO_VOL_DATA), 1e-15);
    assertEquals(0, FUNCTION.getPriceFunction(OTM_PUT).evaluate(ZERO_VOL_DATA), 1e-15);
  }

  public void priceAdjoint() {
    // Price
    double price = FUNCTION.getPriceFunction(ITM_CALL).evaluate(ATM_DATA);
    ValueDerivatives priceAdjoint = FUNCTION.getPriceAdjoint(ITM_CALL, ATM_DATA);
    assertEquals(price, priceAdjoint.getValue(), 1E-10);
    // Price with 0 volatility
    double price0 = FUNCTION.getPriceFunction(ITM_CALL).evaluate(ZERO_VOL_DATA);
    ValueDerivatives price0Adjoint = FUNCTION.getPriceAdjoint(ITM_CALL, ZERO_VOL_DATA);
    assertEquals(price0, price0Adjoint.getValue(), 1E-10);
    // Derivative forward.
    double deltaF = 0.01;
    BlackFunctionData dataFP = BlackFunctionData.of(F + deltaF, DF, SIGMA);
    BlackFunctionData dataFM = BlackFunctionData.of(F - deltaF, DF, SIGMA);
    double priceFP = FUNCTION.getPriceFunction(ITM_CALL).evaluate(dataFP);
    double priceFM = FUNCTION.getPriceFunction(ITM_CALL).evaluate(dataFM);
    double derivativeF_FD = (priceFP - priceFM) / (2 * deltaF);
    assertEquals(derivativeF_FD, priceAdjoint.getDerivative(0), 1E-7);
    // Derivative strike.
    double deltaK = 0.01;
    EuropeanVanillaOption optionKP = EuropeanVanillaOption.of(F - DELTA + deltaK, T, CALL);
    EuropeanVanillaOption optionKM = EuropeanVanillaOption.of(F - DELTA - deltaK, T, CALL);
    double priceKP = FUNCTION.getPriceFunction(optionKP).evaluate(ATM_DATA);
    double priceKM = FUNCTION.getPriceFunction(optionKM).evaluate(ATM_DATA);
    double derivativeK_FD = (priceKP - priceKM) / (2 * deltaK);
    assertEquals(derivativeK_FD, priceAdjoint.getDerivative(2), 1E-7);
    // Derivative volatility.
    double deltaV = 0.0001;
    BlackFunctionData dataVP = BlackFunctionData.of(F, DF, SIGMA + deltaV);
    BlackFunctionData dataVM = BlackFunctionData.of(F, DF, SIGMA - deltaV);
    double priceVP = FUNCTION.getPriceFunction(ITM_CALL).evaluate(dataVP);
    double priceVM = FUNCTION.getPriceFunction(ITM_CALL).evaluate(dataVM);
    double derivativeV_FD = (priceVP - priceVM) / (2 * deltaV);
    assertEquals(derivativeV_FD, priceAdjoint.getDerivative(1), 1E-6);
  }

  public void testPriceAdjointStrike0() {
    // Price
    double price = FUNCTION.getPriceFunction(CALL_0).evaluate(ATM_DATA);
    ValueDerivatives priceAdjoint = FUNCTION.getPriceAdjoint(CALL_0, ATM_DATA);
    assertEquals(price, priceAdjoint.getValue(), 1E-10);
    // Derivative forward.
    double deltaF = 0.01;
    BlackFunctionData dataFP = BlackFunctionData.of(F + deltaF, DF, SIGMA);
    BlackFunctionData dataFM = BlackFunctionData.of(F - deltaF, DF, SIGMA);
    double priceFP = FUNCTION.getPriceFunction(CALL_0).evaluate(dataFP);
    double priceFM = FUNCTION.getPriceFunction(CALL_0).evaluate(dataFM);
    double derivativeF_FD = (priceFP - priceFM) / (2 * deltaF);
    assertEquals(derivativeF_FD, priceAdjoint.getDerivative(0), 1E-7);
    // Derivative strike.
    double deltaK = 0.01;
    EuropeanVanillaOption optionKP = EuropeanVanillaOption.of(0.0 + deltaK, T, CALL);
    double priceKP = FUNCTION.getPriceFunction(optionKP).evaluate(ATM_DATA);
    double derivativeK_FD = (priceKP - price) / (deltaK);
    assertEquals(derivativeK_FD, priceAdjoint.getDerivative(2), 1E-7);
    // Derivative volatility.
    double deltaV = 0.0001;
    BlackFunctionData dataVP = BlackFunctionData.of(F, DF, SIGMA + deltaV);
    BlackFunctionData dataVM = BlackFunctionData.of(F, DF, SIGMA - deltaV);
    double priceVP = FUNCTION.getPriceFunction(CALL_0).evaluate(dataVP);
    double priceVM = FUNCTION.getPriceFunction(CALL_0).evaluate(dataVM);
    double derivativeV_FD = (priceVP - priceVM) / (2 * deltaV);
    assertEquals(derivativeV_FD, priceAdjoint.getDerivative(1), 1E-6);
  }

  private static final double TOLERANCE_1 = 1.0E-10;
  private static final double TOLERANCE_2_FWD_FWD = 1.0E-6;
  private static final double TOLERANCE_2_VOL_VOL = 1.0E-6;
  private static final double TOLERANCE_2_STR_STR = 1.0E-6;
  private static final double TOLERANCE_2_FWD_VOL = 1.0E-7;
  private static final double TOLERANCE_2_FWD_STR = 1.0E-6;
  private static final double TOLERANCE_2_STR_VOL = 1.0E-6;

  /** Tests second order Algorithmic Differentiation version of BlackFunction with several data sets. */
  public void testPriceAdjoint2() {
    // forward, numeraire, sigma, strike, time
    double[][] testData = {
        {104.0d, 0.9d, 0.50d, 94.0d, 4.5d},
        {104.0d, 0.9d, 0.50d, 124.0d, 4.5d},
        {104.0d, 0.9d, 0.50d, 104.0d, 4.5d},
        {0.0250d, 1000.0d, 0.25d, 0.0150d, 10.0d},
        {0.0250d, 1000.0d, 0.25d, 0.0400d, 10.0d},
        {1700.0d, 0.9d, 1.00d, 1500.0d, 0.01d},
        {1700.0d, 0.9d, 1.00d, 1900.0d, 20.0d}
    };
    int nbTest = testData.length;
    for (int i = 0; i < nbTest; i++) {
      testPriceAdjointSecondOrder(testData[i][0], testData[i][1], testData[i][2], testData[i][3], testData[i][4], CALL, i);
      testPriceAdjointSecondOrder(testData[i][0], testData[i][1], testData[i][2], testData[i][3], testData[i][4], PUT, i);
    }
  }

  private void testPriceAdjointSecondOrder(double forward, double numeraire, double sigma, double strike, double time,
      PutCall putCall, int i) {
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, time, putCall);
    BlackFunctionData data = BlackFunctionData.of(forward, numeraire, sigma);
    // Price
    ValueDerivatives priceAdjoint = FUNCTION.getPriceAdjoint(option, data);
    double[] bsD = new double[3];
    double[][] bsD2 = new double[3][3];
    double bs = FUNCTION.getPriceAdjoint2(option, data, bsD, bsD2);
    assertEquals("AD Second order: price", priceAdjoint.getValue(), bs, TOLERANCE_1);
    // First derivative
    for (int loopder = 0; loopder < 3; loopder++) {
      assertEquals("AD Second order: 1st", priceAdjoint.getDerivatives()[loopder], bsD[loopder], TOLERANCE_1);
    }
    // Second derivative
    // Derivative forward-forward.
    double deltaF = 1.0E-3 * forward;
    BlackFunctionData dataFP = BlackFunctionData.of(forward + deltaF, numeraire, sigma);
    BlackFunctionData dataFM = BlackFunctionData.of(forward - deltaF, numeraire, sigma);
    ValueDerivatives priceAdjointFP = FUNCTION.getPriceAdjoint(option, dataFP);
    ValueDerivatives priceAdjointFM = FUNCTION.getPriceAdjoint(option, dataFM);
    double derivativeFF_FD = (priceAdjointFP.getDerivative(0) - priceAdjointFM.getDerivative(0)) / (2 * deltaF);
    assertEquals("AD Second order: 2nd - fwd-fwd " + i,
        derivativeFF_FD, bsD2[0][0], TOLERANCE_2_FWD_FWD * Math.abs(bs / (deltaF * deltaF)));
    // Derivative volatility-volatility.
    double deltaV = 0.00001;
    double deltaV2 = (deltaV * deltaV);
    BlackFunctionData dataVP = BlackFunctionData.of(forward, numeraire, sigma + deltaV);
    BlackFunctionData dataVM = BlackFunctionData.of(forward, numeraire, sigma - deltaV);
    ValueDerivatives priceAdjointVP = FUNCTION.getPriceAdjoint(option, dataVP);
    ValueDerivatives priceAdjointVM = FUNCTION.getPriceAdjoint(option, dataVM);
    double derivativeVV_FD = (priceAdjointVP.getDerivative(1) - priceAdjointVM.getDerivative(1)) / (2 * deltaV);
    assertEquals("AD Second order: 2nd - vol-vol " + i,
        derivativeVV_FD, bsD2[1][1], TOLERANCE_2_VOL_VOL * Math.abs(bs / deltaV2));
    // Derivative forward-volatility.
    double derivativeFV_FD = (priceAdjointVP.getDerivative(0) - priceAdjointVM.getDerivative(0)) / (2 * deltaV);
    assertEquals("AD Second order: 2nd - fwd-vol " + i,
        derivativeFV_FD, bsD2[1][0], TOLERANCE_2_FWD_VOL * Math.abs(bs / (deltaF * deltaV)));
    assertEquals("AD Second order: 2nd - fwd-vol", bsD2[0][1], bsD2[1][0], TOLERANCE_1);
    // Derivative strike-strike.
    double deltaK = 1.0E-4 * strike;
    EuropeanVanillaOption optionKP = EuropeanVanillaOption.of(strike + deltaK, time, putCall);
    EuropeanVanillaOption optionKM = EuropeanVanillaOption.of(strike - deltaK, time, putCall);
    ValueDerivatives priceAdjointKP = FUNCTION.getPriceAdjoint(optionKP, data);
    ValueDerivatives priceAdjointKM = FUNCTION.getPriceAdjoint(optionKM, data);
    double derivativeKK_FD = (priceAdjointKP.getDerivative(2) - priceAdjointKM.getDerivative(2)) / (2 * deltaK);
    assertEquals("AD Second order: 2nd - strike-strike " + i,
        derivativeKK_FD, bsD2[2][2], TOLERANCE_2_STR_STR * Math.abs(derivativeKK_FD));
    // Derivative forward-strike.
    double derivativeFK_FD = (priceAdjointKP.getDerivative(0) - priceAdjointKM.getDerivative(0)) / (2 * deltaK);
    assertEquals("AD Second order: 2nd - fwd-str " + i,
        derivativeFK_FD, bsD2[2][0], TOLERANCE_2_FWD_STR * Math.abs(bs / (deltaF * deltaK)));
    assertEquals("AD Second order: 2nd - fwd-str", bsD2[0][2], bsD2[2][0], TOLERANCE_1);
    // Derivative strike-volatility.
    double derivativeKV_FD = (priceAdjointVP.getDerivative(2) - priceAdjointVM.getDerivative(2)) / (2 * deltaV);
    assertEquals("AD Second order: 2nd - str-vol " + i,
        derivativeKV_FD, bsD2[2][1], TOLERANCE_2_STR_VOL * Math.abs(bs / (deltaV * deltaK)));
    assertEquals("AD Second order: 2nd - str-vol", bsD2[1][2], bsD2[2][1], TOLERANCE_1);
  }

}
