/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
@Test
public class DiscountingIborFutureTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DiscountingIborFutureTradePricer PRICER_TRADE = DiscountingIborFutureTradePricer.DEFAULT;
  private static final DiscountingIborFutureProductPricer PRICER_PRODUCT = DiscountingIborFutureProductPricer.DEFAULT;
  private static final ResolvedIborFutureTrade FUTURE_TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE.resolve(REF_DATA);
  private static final ResolvedIborFuture FUTURE = FUTURE_TRADE.getProduct();

  private static final double RATE = 0.045;

  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;
  private static final double TOLERANCE_PV = 1.0e-4;
  private static final double TOLERANCE_PV_DELTA = 1.0e-2;

  //------------------------------------------------------------------------- 
  public void test_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    assertEquals(PRICER_TRADE.price(FUTURE_TRADE, prov), 1.0 - RATE, TOLERANCE_PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_parSpread_after_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradeInfo().getTradeDate().get().plusDays(1));
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);
    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(FUTURE_TRADE, prov) - lastClosingPrice;
    double parSpreadComputed = PRICER_TRADE.parSpread(FUTURE_TRADE, prov, lastClosingPrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }
  
  public void test_parSpread_on_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradeInfo().getTradeDate().get());
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(FUTURE_TRADE, prov) - FUTURE_TRADE.getPrice();
    double parSpreadComputed = PRICER_TRADE.parSpread(FUTURE_TRADE, prov, lastClosingPrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }

  //------------------------------------------------------------------------- 
  public void test_presentValue_after_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradeInfo().getTradeDate().get().plusDays(1));
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 1.025;
    DiscountingIborFutureTradePricer pricerFn = DiscountingIborFutureTradePricer.DEFAULT;
    double expected = ((1.0 - RATE) - lastClosingPrice) *
        FUTURE.getAccrualFactor() * FUTURE.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(FUTURE_TRADE, prov, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
    assertEquals(computed.getCurrency(), FUTURE.getCurrency());
  }

  public void test_presentValue_on_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradeInfo().getTradeDate().get());
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 1.025;
    DiscountingIborFutureTradePricer pricerFn = DiscountingIborFutureTradePricer.DEFAULT;
    double expected = ((1.0 - RATE) - FUTURE_TRADE.getPrice()) *
        FUTURE.getAccrualFactor() * FUTURE.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(FUTURE_TRADE, prov, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
    assertEquals(computed.getCurrency(), FUTURE.getCurrency());
  }

  //-------------------------------------------------------------------------   
  public void test_presentValueSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiPrice = PRICER_PRODUCT.priceSensitivity(FUTURE, prov);
    PointSensitivities sensiPresentValueExpected = sensiPrice.multipliedBy(
        FUTURE.getNotional() * FUTURE.getAccrualFactor() * FUTURE_TRADE.getQuantity());
    PointSensitivities sensiPresentValueComputed = PRICER_TRADE.presentValueSensitivity(FUTURE_TRADE, prov);
    assertTrue(sensiPresentValueComputed.equalWithTolerance(sensiPresentValueExpected, TOLERANCE_PV_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_parSpreadSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(FUTURE, prov);
    PointSensitivities sensiComputed = PRICER_TRADE.parSpreadSensitivity(FUTURE_TRADE, prov);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA));
  }

}
