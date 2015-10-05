/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;

/**
 * Test {@link NormalImpliedVolatilityFormula}.
 */
@Test
public class NormalImpliedVolatilityFormulaTest {

  private static final double FORWARD = 100.0;
  private static final double DF = 0.87;
  private static final double T = 4.5;
  private static final NormalFunctionData[] DATA;
  private static final EuropeanVanillaOption[] OPTIONS;
  private static final double[] PRICES;
  private static final double[] STRIKES;
  private static final double[] SIGMA;
  private static final NormalPriceFunction FUNCTION = new NormalPriceFunction();
  private static final int N = 10;

  static {
    PRICES = new double[N];
    STRIKES = new double[N];
    SIGMA = new double[N];
    DATA = new NormalFunctionData[N];
    OPTIONS = new EuropeanVanillaOption[N];
    for (int i = 0; i < N; i++) {
      STRIKES[i] = FORWARD + (-N / 2 + i) * 10;
      SIGMA[i] = FORWARD * (0.05 + 4.0 * i / 100.0);
      DATA[i] = NormalFunctionData.of(FORWARD, DF, SIGMA[i]);
      OPTIONS[i] = EuropeanVanillaOption.of(STRIKES[i], T, PutCall.CALL);
      PRICES[i] = FUNCTION.getPriceFunction(OPTIONS[i]).evaluate(DATA[i]);
    }
  }

  private static final NormalImpliedVolatilityFormula FORMULA_IMPLIED = NormalImpliedVolatilityFormula.DEFAULT;

  public void implied_volatility() {
    double[] impliedVolatility = new double[N];
    for (int i = 0; i < N; i++) {
      impliedVolatility[i] = FORMULA_IMPLIED.impliedVolatility(DATA[i], OPTIONS[i], PRICES[i]);
      assertEquals(SIGMA[i], impliedVolatility[i], 1e-6);
    }
  }

  public void intrinsic_price() {
    NormalFunctionData data = NormalFunctionData.of(1.0, 1.0, 0.01);
    EuropeanVanillaOption option1 = EuropeanVanillaOption.of(0.5, 1.0, PutCall.CALL);
    assertThrowsIllegalArg(() -> FORMULA_IMPLIED.impliedVolatility(data, option1, 1e-6));
    EuropeanVanillaOption option2 = EuropeanVanillaOption.of(1.5, 1.0, PutCall.PUT);
    assertThrowsIllegalArg(() -> FORMULA_IMPLIED.impliedVolatility(data, option2, 1e-6));
  }

}
