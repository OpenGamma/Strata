/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Tests {@link AbstractIborFutureTradePricer}.
 */
@Test
public class AbstractIborFutureTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ResolvedIborFutureTrade FUTURE_TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE.resolve(REF_DATA);
  private static final AbstractIborFutureTradePricer PRICER = DiscountingIborFutureTradePricer.DEFAULT;
  private static final double TOLERANCE_PV = 1.0E-4;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER.getProductPricer()
        .marginIndex(FUTURE_TRADE.getProduct(), currentPrice);
    double referencePriceIndex = PRICER.getProductPricer()
        .marginIndex(FUTURE_TRADE.getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * FUTURE_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER.presentValue(FUTURE_TRADE, currentPrice, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_reference_price_after_trade_date() {
    LocalDate tradeDate = FUTURE_TRADE.getTradeInfo().getTradeDate().get();
    LocalDate valuationDate = tradeDate.plusDays(1);
    double lastMarginPrice = 0.995;
    double referencePrice = PRICER.referencePrice(FUTURE_TRADE, valuationDate, lastMarginPrice);
    assertEquals(referencePrice, lastMarginPrice);
  }
  
  public void test_reference_price_on_trade_date() {
    LocalDate tradeDate = FUTURE_TRADE.getTradeInfo().getTradeDate().get();
    LocalDate valuationDate = tradeDate;
    double lastMarginPrice = 0.995;
    double referencePrice = PRICER.referencePrice(FUTURE_TRADE, valuationDate, lastMarginPrice);
    assertEquals(referencePrice, FUTURE_TRADE.getPrice());
  }
  
  public void test_reference_price_val_date_not_null() {
    double lastMarginPrice = 0.995;
    assertThrowsIllegalArg(() -> PRICER.referencePrice(FUTURE_TRADE, null, lastMarginPrice));
  }

}
