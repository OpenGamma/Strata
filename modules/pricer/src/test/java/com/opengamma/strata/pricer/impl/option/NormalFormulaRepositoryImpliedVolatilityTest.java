/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link NormalFormulaRepository} implied volatility.
 */
public class NormalFormulaRepositoryImpliedVolatilityTest {

  private static final double FORWARD = 100.0;
  private static final double DF = 0.87;
  private static final double T = 4.5;
  private static final NormalFunctionData[] DATA;
  private static final int N = 10;
  private static final double[] PRICES;
  private static final double[] STRIKES = new double[N];
  private static final double[] STRIKES_ATM = new double[N];
  private static final EuropeanVanillaOption[] OPTIONS = new EuropeanVanillaOption[N];
  private static final double[] SIGMA;
  private static final double[] SIGMA_BLACK = new double[N];
  private static final NormalPriceFunction FUNCTION = new NormalPriceFunction();
  static {
    PRICES = new double[N];
    SIGMA = new double[N];
    DATA = new NormalFunctionData[N];
    for (int i = 0; i < N; i++) {
      STRIKES[i] = FORWARD + (-N / 2 + i) * 10;
      STRIKES_ATM[i] = FORWARD + (-0.5d * N + i) / 100.0d;
      SIGMA[i] = FORWARD * (0.05 + 4.0 * i / 100.0);
      SIGMA_BLACK[i] = 0.20 + i / 100.0d;
      DATA[i] = NormalFunctionData.of(FORWARD, DF, SIGMA[i]);
      OPTIONS[i] = EuropeanVanillaOption.of(STRIKES[i], T, PutCall.CALL);
      PRICES[i] = FUNCTION.getPriceFunction(OPTIONS[i]).apply(DATA[i]);
    }
  }
  private static final double TOLERANCE_PRICE = 1.0E-4;
  private static final double TOLERANCE_VOL = 1.0E-6;

  @Test
  public void implied_volatility() {
    double[] impliedVolatility = new double[N];
    for (int i = 0; i < N; i++) {
      impliedVolatility[i] = impliedVolatility(DATA[i], OPTIONS[i], PRICES[i]);
      assertThat(impliedVolatility[i]).isCloseTo(SIGMA[i], offset(1e-6));
    }
  }
  
  @Test
  public void implied_volatility_negative() {
    double forward = -0.0050; // 50 bps
    double[] strikes = {0.0150, -0.0100, -0.0050, -0.00};
    int nbTests = strikes.length;
    double[] volatilities = {0.0075, 0.0100, 0.0050, 0.0200};
    double[] volatilitiesStart = {0.0100, 0.0010, 0.0049, 0.0400};
    double[] timesToExpiry = {1.0, 5.0, 0.25, 1.0};
    PutCall[] putCall = {PutCall.PUT, PutCall.CALL, PutCall.CALL, PutCall.PUT};
    for (int i = 0; i < nbTests; i++) {
      double price = NormalFormulaRepository
          .price(forward, strikes[i], timesToExpiry[i], volatilities[i], putCall[i]);
      double ivComputed = NormalFormulaRepository
          .impliedVolatility(price, forward, strikes[i], timesToExpiry[i], volatilitiesStart[i], 1.0d, putCall[i]);
      assertThat(ivComputed).isCloseTo(volatilities[i], offset(1e-8));   
    }
  }

  @Test
  public void intrinsic_price() {
    NormalFunctionData data = NormalFunctionData.of(1.0, 1.0, 0.01);
    EuropeanVanillaOption option1 = EuropeanVanillaOption.of(0.5, 1.0, PutCall.CALL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> impliedVolatility(data, option1, 1e-6));
    EuropeanVanillaOption option2 = EuropeanVanillaOption.of(1.5, 1.0, PutCall.PUT);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> impliedVolatility(data, option2, 1e-6));
  }

  private double impliedVolatility(
      NormalFunctionData data,
      EuropeanVanillaOption option,
      double price) {

    return NormalFormulaRepository.impliedVolatility(
        price,
        data.getForward(),
        option.getStrike(),
        option.getTimeToExpiry(),
        data.getNormalVolatility(),
        data.getNumeraire(),
        option.getPutCall());
  }

  @Test
  public void wrong_strike() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NormalFormulaRepository.impliedVolatilityFromBlackApproximated(FORWARD, -1.0d, T, 0.20d));
  }

  @Test
  public void wrong_forward() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NormalFormulaRepository.impliedVolatilityFromBlackApproximated(-1.0d, FORWARD, T, 0.20d));
  }

  @Test
  public void price_comparison() {
    priceCheck(STRIKES);
    priceCheck(STRIKES_ATM);
  }

  private void priceCheck(double[] strikes) {
    for (int i = 0; i < N; i++) {
      double ivNormalComputed = NormalFormulaRepository
          .impliedVolatilityFromBlackApproximated(FORWARD, strikes[i], T, SIGMA_BLACK[i]);
      double priceNormalComputed = 
          NormalFormulaRepository.price(FORWARD, strikes[i], T, ivNormalComputed, PutCall.CALL) * DF;
      double priceBlack = BlackFormulaRepository.price(FORWARD, strikes[i], T, SIGMA_BLACK[i], true) * DF;
      assertThat(priceNormalComputed).isCloseTo(priceBlack, offset(TOLERANCE_PRICE));
    }
  }

  @Test
  public void implied_volatility_adjoint() {
    double shiftFd = 1.0E-6;
    for (int i = 0; i < N; i++) {
      double impliedVol =
          NormalFormulaRepository.impliedVolatilityFromBlackApproximated(FORWARD, STRIKES[i], T, SIGMA_BLACK[i]);
      ValueDerivatives impliedVolAdj =
          NormalFormulaRepository.impliedVolatilityFromBlackApproximatedAdjoint(FORWARD, STRIKES[i], T, SIGMA_BLACK[i]);
      assertThat(impliedVolAdj.getValue()).isCloseTo(impliedVol, offset(TOLERANCE_VOL));
      double impliedVolP =
          NormalFormulaRepository.impliedVolatilityFromBlackApproximated(FORWARD, STRIKES[i], T, SIGMA_BLACK[i] + shiftFd);
      double impliedVolM =
          NormalFormulaRepository.impliedVolatilityFromBlackApproximated(FORWARD, STRIKES[i], T, SIGMA_BLACK[i] - shiftFd);
      double derivativeApproximated = (impliedVolP - impliedVolM) / (2 * shiftFd);
      assertThat(impliedVolAdj.getDerivatives().size()).isEqualTo(1);
      assertThat(impliedVolAdj.getDerivative(0)).isCloseTo(derivativeApproximated, offset(TOLERANCE_VOL));
    }
  }

}
