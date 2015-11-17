/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.pricer.index.IborFutureDummyData.IBOR_FUTURE_TRADE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Tests {@link AbstractIborFutureTradePricer}.
 */
@Test
public class AbstractIborFutureTradePricerTest {

  private static final AbstractIborFutureTradePricer PRICER = DiscountingIborFutureTradePricer.DEFAULT;
  private static final double TOLERANCE_PV = 1.0E-4;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER.getProductPricer()
        .marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), currentPrice);
    double referencePriceIndex = PRICER.getProductPricer()
        .marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * IBOR_FUTURE_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER.presentValue(IBOR_FUTURE_TRADE, currentPrice, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_reference_price_after_trade_date() {
    LocalDate tradeDate = IBOR_FUTURE_TRADE.getTradeInfo().getTradeDate().get();
    LocalDate valuationDate = tradeDate.plusDays(1);
    double lastMarginPrice = 0.995;
    double referencePrice = PRICER.referencePrice(IBOR_FUTURE_TRADE, valuationDate, lastMarginPrice);
    assertEquals(referencePrice, lastMarginPrice);
  }
  
  public void test_reference_price_on_trade_date() {
    LocalDate tradeDate = IBOR_FUTURE_TRADE.getTradeInfo().getTradeDate().get();
    LocalDate valuationDate = tradeDate;
    double lastMarginPrice = 0.995;
    double referencePrice = PRICER.referencePrice(IBOR_FUTURE_TRADE, valuationDate, lastMarginPrice);
    assertEquals(referencePrice, IBOR_FUTURE_TRADE.getInitialPrice());
  }
  
  public void test_reference_price_val_date_not_null() {
    double lastMarginPrice = 0.995;
    assertThrowsIllegalArg(() -> PRICER.referencePrice(IBOR_FUTURE_TRADE, null, lastMarginPrice));
  }

}
