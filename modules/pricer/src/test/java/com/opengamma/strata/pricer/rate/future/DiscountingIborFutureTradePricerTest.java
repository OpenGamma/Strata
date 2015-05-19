/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
@Test
public class DiscountingIborFutureTradePricerTest {

  private static final DiscountingIborFutureTradePricer PRICER_TRADE = DiscountingIborFutureTradePricer.DEFAULT;
  private static final DiscountingIborFutureProductPricer PRICER_PRODUCT = DiscountingIborFutureProductPricer.DEFAULT;
  private static final IborFutureTrade FUTURE_TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE;
  private static final IborFuture FUTURE_PRODUCT = FUTURE_TRADE.getSecurity().getProduct();

  private static final double RATE = 0.045;
  private static final RatesProvider MOCK_PROV = mock(RatesProvider.class);
  static {
    when(MOCK_PROV.iborIndexRate(FUTURE_TRADE.getSecurity().getProduct().getIndex(),
        FUTURE_TRADE.getSecurity().getProduct().getFixingDate())).thenReturn(RATE);
  }

  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;
  private static final double TOLERANCE_PV = 1.0e-4;
  private static final double TOLERANCE_PV_DELTA = 1.0e-2;

  //------------------------------------------------------------------------- 
  public void test_price() {
    assertEquals(PRICER_TRADE.price(FUTURE_TRADE, MOCK_PROV), 1.0 - RATE, TOLERANCE_PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_parSpread() {
    double referencePrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(FUTURE_TRADE, MOCK_PROV) - referencePrice;
    double parSpreadComputed = PRICER_TRADE.parSpread(FUTURE_TRADE, MOCK_PROV, referencePrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }

  //------------------------------------------------------------------------- 
  public void test_presentValue() {
    double lastClosingPrice = 1.025;
    IborFuture future = FUTURE_TRADE.getSecurity().getProduct();
    DiscountingIborFutureTradePricer pricerFn = DiscountingIborFutureTradePricer.DEFAULT;
    double expected = ((1.0 - RATE) - lastClosingPrice) *
        future.getAccrualFactor() * future.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(FUTURE_TRADE, MOCK_PROV, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
    assertEquals(computed.getCurrency(), future.getCurrency());
  }

  //-------------------------------------------------------------------------   
  public void test_presentValueSensitivity() {
    PointSensitivities sensiPrice = PRICER_PRODUCT.priceSensitivity(FUTURE_PRODUCT, MOCK_PROV);
    PointSensitivities sensiPresentValueExpected = sensiPrice.multipliedBy(
        FUTURE_PRODUCT.getNotional() * FUTURE_PRODUCT.getAccrualFactor() * FUTURE_TRADE.getQuantity());
    PointSensitivities sensiPresentValueComputed = PRICER_TRADE.presentValueSensitivity(FUTURE_TRADE, MOCK_PROV);
    assertTrue(sensiPresentValueComputed.equalWithTolerance(sensiPresentValueExpected, TOLERANCE_PV_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_parSpreadSensitivity() {
    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(FUTURE_PRODUCT, MOCK_PROV);
    PointSensitivities sensiComputed = PRICER_TRADE.parSpreadSensitivity(FUTURE_TRADE, MOCK_PROV);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA));
  }

}
