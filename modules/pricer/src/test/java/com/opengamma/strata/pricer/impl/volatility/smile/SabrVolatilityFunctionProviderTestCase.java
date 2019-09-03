/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.product.common.PutCall.CALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;

/**
 * Test case for SABR volatility function providers.
 */
public abstract class SabrVolatilityFunctionProviderTestCase {

  private static final double K = 105;
  private static final double T = 1.5;
  protected static final double FORWARD = 103;
  protected static final EuropeanVanillaOption OPTION = EuropeanVanillaOption.of(K, T, CALL);
  protected static final SabrFormulaData LOG_NORMAL_EQUIVALENT = SabrFormulaData.of(0.8, 1, 0.5, 0);
  protected static final SabrFormulaData APPROACHING_LOG_NORMAL_EQUIVALENT1 = SabrFormulaData.of(0.8, 1, 0.5, 1e-6);
  protected static final SabrFormulaData APPROACHING_LOG_NORMAL_EQUIVALENT2 = SabrFormulaData.of(0.8, 1 + 1e-6, 0.5, 0);
  protected static final SabrFormulaData APPROACHING_LOG_NORMAL_EQUIVALENT3 = SabrFormulaData.of(0.8, 1 - 1e-6, 0.5, 0);

  protected abstract VolatilityFunctionProvider<SabrFormulaData> getFunction();

  @Test
  public void testNullData() {
    assertThatIllegalArgumentException().isThrownBy(() -> getFunction().volatility(FORWARD, K, T, null));
  }

  @Test
  public void testLogNormalEquivalent() {
    assertThat(getFunction().volatility(FORWARD, K, T, LOG_NORMAL_EQUIVALENT))
        .isEqualTo(LOG_NORMAL_EQUIVALENT.getAlpha());
  }

  @Test
  public void testApproachingLogNormalEquivalent1() {
    assertThat(getFunction().volatility(FORWARD, K, T, APPROACHING_LOG_NORMAL_EQUIVALENT1))
        .isCloseTo(LOG_NORMAL_EQUIVALENT.getAlpha(), offset(1e-5));
  }

  @Test
  public void testApproachingLogNormalEquivalent2() {
    assertThat(getFunction().volatility(FORWARD, K, T, APPROACHING_LOG_NORMAL_EQUIVALENT2))
        .isCloseTo(LOG_NORMAL_EQUIVALENT.getAlpha(), offset(1e-5));
  }

  @Test
  public void testApproachingLogNormalEquivalent3() {
    assertThat(getFunction().volatility(FORWARD, K, T, APPROACHING_LOG_NORMAL_EQUIVALENT3))
        .isCloseTo(LOG_NORMAL_EQUIVALENT.getAlpha(), offset(1e-5));
  }

  //TODO need to fill in tests
  //TODO beta = 1 nu = 0 => Black equivalent volatility
  //TODO beta = 0 nu = 0 => Bachelier
  //TODO beta != 0 nu = 0 => CEV

}
