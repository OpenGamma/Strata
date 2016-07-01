/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.model;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;

/**
 * Test {@link HullWhiteOneFactorPiecewiseConstantParameters}.
 */
@Test
public class HullWhiteOneFactorPiecewiseConstantParametersTest {

  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);

  public void test_of() {
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    assertEquals(test.getLastVolatility(), VOLATILITY.get(VOLATILITY.size() - 1));
    assertEquals(test.getMeanReversion(), MEAN_REVERSION);
    assertEquals(test.getVolatility(), VOLATILITY);
    assertEquals(test.getVolatilityTime(), DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 1000d));
  }

  public void test_of_noTime() {
    double eta = 0.02;
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, DoubleArray.of(eta), DoubleArray.of());
    assertEquals(test.getLastVolatility(), eta);
    assertEquals(test.getMeanReversion(), MEAN_REVERSION);
    assertEquals(test.getVolatility(), DoubleArray.of(eta));
    assertEquals(test.getVolatilityTime(), DoubleArray.of(0d, 1000d));
  }

  public void test_setVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    DoubleArray newVol = DoubleArray.of(0.04, 0.012, 0.016, 0.019, 0.024);
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withVolatility(newVol);
    assertEquals(test.getLastVolatility(), newVol.get(newVol.size() - 1));
    assertEquals(test.getMeanReversion(), base.getMeanReversion());
    assertEquals(test.getVolatility(), newVol);
    assertEquals(test.getVolatilityTime(), base.getVolatilityTime());
  }

  public void test_setLastVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double lastVol = 0.092;
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withLastVolatility(lastVol);
    assertEquals(test.getLastVolatility(), lastVol);
    assertEquals(test.getMeanReversion(), base.getMeanReversion());
    assertEquals(test.getVolatility(), DoubleArray.of(0.01, 0.011, 0.012, 0.013, lastVol));
    assertEquals(test.getVolatilityTime(), base.getVolatilityTime());
  }

  public void test_addVolatility() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double time = 7.0;
    double vol = 0.015;
    HullWhiteOneFactorPiecewiseConstantParameters test = base.withVolatilityAdded(vol, time);
    assertEquals(test.getLastVolatility(), vol);
    assertEquals(test.getMeanReversion(), MEAN_REVERSION);
    assertEquals(test.getVolatility(), DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014, vol));
    assertEquals(test.getVolatilityTime(), DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 7.0, 1000d));
  }

  public void test_of_notAscendingTime() {
    DoubleArray time = DoubleArray.of(0.5, 1.0, 4.0, 2.0);
    assertThrowsIllegalArg(() -> HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, time));
  }

  public void test_of_notAscendingTime1() {
    DoubleArray time = DoubleArray.of(0.5, 1.0, 4.0);
    assertThrowsIllegalArg(() -> HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, time));
  }

  public void test_addVolatility_notAscendingTime() {
    HullWhiteOneFactorPiecewiseConstantParameters base =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    double time = 3.0;
    double vol = 0.015;
    assertThrowsIllegalArg(() -> base.withVolatilityAdded(vol, time));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HullWhiteOneFactorPiecewiseConstantParameters test1 =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    coverImmutableBean(test1);
    HullWhiteOneFactorPiecewiseConstantParameters test2 =
        HullWhiteOneFactorPiecewiseConstantParameters.of(0.02, DoubleArray.of(0.015), DoubleArray.of());
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    HullWhiteOneFactorPiecewiseConstantParameters test =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    assertSerialization(test);
  }
}
