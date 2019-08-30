/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.model;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;

/**
 * Test {@link HullWhiteOneFactorPiecewiseConstantParameters}.
 */
public class HullWhiteOneFactorPiecewiseConstantParametersTest {

  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);

  @Test
  public void test_of() {
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    assertThat(test.getLastVolatility()).isEqualTo(VOLATILITY.get(VOLATILITY.size() - 1));
    assertThat(test.getMeanReversion()).isEqualTo(MEAN_REVERSION);
    assertThat(test.getVolatility()).isEqualTo(VOLATILITY);
    assertThat(test.getVolatilityTime()).isEqualTo(DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 1000d));
  }

  @Test
  public void test_of_noTime() {
    double eta = 0.02;
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, DoubleArray.of(eta), DoubleArray.of());
    assertThat(test.getLastVolatility()).isEqualTo(eta);
    assertThat(test.getMeanReversion()).isEqualTo(MEAN_REVERSION);
    assertThat(test.getVolatility()).isEqualTo(DoubleArray.of(eta));
    assertThat(test.getVolatilityTime()).isEqualTo(DoubleArray.of(0d, 1000d));
  }

  @Test
  public void test_setVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    DoubleArray newVol = DoubleArray.of(0.04, 0.012, 0.016, 0.019, 0.024);
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withVolatility(newVol);
    assertThat(test.getLastVolatility()).isEqualTo(newVol.get(newVol.size() - 1));
    assertThat(test.getMeanReversion()).isEqualTo(base.getMeanReversion());
    assertThat(test.getVolatility()).isEqualTo(newVol);
    assertThat(test.getVolatilityTime()).isEqualTo(base.getVolatilityTime());
  }

  @Test
  public void test_setLastVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double lastVol = 0.092;
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withLastVolatility(lastVol);
    assertThat(test.getLastVolatility()).isEqualTo(lastVol);
    assertThat(test.getMeanReversion()).isEqualTo(base.getMeanReversion());
    assertThat(test.getVolatility()).isEqualTo(DoubleArray.of(0.01, 0.011, 0.012, 0.013, lastVol));
    assertThat(test.getVolatilityTime()).isEqualTo(base.getVolatilityTime());
  }

  @Test
  public void test_addVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double time = 7.0;
    double vol = 0.015;
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withVolatilityAdded(vol, time);
    assertThat(test.getLastVolatility()).isEqualTo(vol);
    assertThat(test.getMeanReversion()).isEqualTo(MEAN_REVERSION);
    assertThat(test.getVolatility()).isEqualTo(DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014, vol));
    assertThat(test.getVolatilityTime()).isEqualTo(DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 7.0, 1000d));
  }

  @Test
  public void test_of_notAscendingTime() {
    DoubleArray time = DoubleArray.of(0.5, 1.0, 4.0, 2.0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, time));
  }

  @Test
  public void test_of_notAscendingTime1() {
    DoubleArray time = DoubleArray.of(0.5, 1.0, 4.0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, time));
  }

  @Test
  public void test_addVolatility_notAscendingTime() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double time = 3.0;
    double vol = 0.015;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withVolatilityAdded(vol, time));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HullWhiteOneFactorPiecewiseConstantParameters test1 =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    coverImmutableBean(test1);
    HullWhiteOneFactorPiecewiseConstantParameters test2 =
        HullWhiteOneFactorPiecewiseConstantParameters.of(0.02, DoubleArray.of(0.015), DoubleArray.of());
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    assertSerialization(test);
  }
}
