/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.pricer.RatesProvider;

/**
 * Test.
 */
@Test
public class DiscountingFxResetNotionalExchangePricerTest {

  public void test_presentValue() {
    double discountFactor = 0.98d;
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    when(mockProv.discountFactor(ne.getCurrency(), ne.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(
        test.presentValue(ne, mockProv),
        ne.getNotional() * 1.6d * discountFactor, 0d);
  }

  public void test_futureValue() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(
        test.futureValue(ne, mockProv),
        ne.getNotional() * 1.6d, 0d);
  }

}
