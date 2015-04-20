/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.pricer.rate.future.IborFutureDummyData.IBOR_FUTURE_TRADE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Tests {@link BaseIborFutureTradePricer}.
 */
@Test
public class BaseIborFutureTradePricerTest {

  private static final BaseIborFutureTradePricer PRICER = DiscountingIborFutureTradePricer.DEFAULT;
  private static final double TOLERANCE_PV = 1.0E-4;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER.getIborFutureProductPricer()
        .marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), currentPrice);
    double referencePriceIndex = PRICER.getIborFutureProductPricer()
        .marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * IBOR_FUTURE_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER.presentValue(IBOR_FUTURE_TRADE, currentPrice, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

}
