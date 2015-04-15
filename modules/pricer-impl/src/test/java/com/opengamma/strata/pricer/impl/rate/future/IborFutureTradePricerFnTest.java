/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.future.IborFutureProductPricerFn;
import com.opengamma.strata.pricer.rate.future.IborFutureTradePricerFn;

/**
 * Tests {@link IborFutureTradePricerFn}.
 */
public class IborFutureTradePricerFnTest {

  private static final IborFutureTradePricerFn PRICER_TRADE = DefaultIborFutureTradePricerFn.DEFAULT;
  private static final IborFutureProductPricerFn PRICER_PRODUCT = DefaultIborFutureProductPricerFn.DEFAULT;
  private static final IborFutureTrade TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE;

  private static final double RATE = 0.045;
  private static final PricingEnvironment ENV_MOCK = mock(PricingEnvironment.class);
  static {
    when(ENV_MOCK.iborIndexRate(TRADE.getSecurity().getProduct().getIndex(),
        TRADE.getSecurity().getProduct().getLastTradeDate())).thenReturn(RATE);
  }

  private static final double TOLERANCE_PV = 1.0E-4;

  //------------------------------------------------------------------------- 
  @Test
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER_PRODUCT.marginIndex(TRADE.getSecurity().getProduct(), currentPrice);
    double referencePriceIndex = PRICER_PRODUCT.marginIndex(TRADE.getSecurity().getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER_TRADE.presentValue(currentPrice, TRADE, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

}
