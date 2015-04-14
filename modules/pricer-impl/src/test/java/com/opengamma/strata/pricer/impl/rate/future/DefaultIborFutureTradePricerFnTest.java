/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.future.IborFutureProductPricerFn;
import com.opengamma.strata.pricer.rate.future.IborFutureTradePricerFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Test {@link DefaultIborFutureTradePricerFn}.
 */
@Test
public class DefaultIborFutureTradePricerFnTest {

  private static final IborFutureTradePricerFn PRICER_TRADE = DefaultIborFutureTradePricerFn.DEFAULT;
  private static final IborFutureProductPricerFn PRICER_PRODUCT = DefaultIborFutureProductPricerFn.DEFAULT;
  private static final IborFutureTrade FUTURE_TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE;
  private static final IborFuture FUTURE_PRODUCT = FUTURE_TRADE.getSecurity().getProduct();
  
  private static final double RATE = 0.045;
  private static final PricingEnvironment ENV_MOCK = mock(PricingEnvironment.class);
  static {
    when(ENV_MOCK.iborIndexRate(FUTURE_TRADE.getSecurity().getProduct().getIndex(), 
        FUTURE_TRADE.getSecurity().getProduct().getLastTradeDate())).thenReturn(RATE);
  }  

  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;
  private static final double TOLERANCE_PV = 1.0e-4;
  private static final double TOLERANCE_PV_DELTA = 1.0e-2;

  //------------------------------------------------------------------------- 
  public void test_price() {
    assertEquals(PRICER_TRADE.price(ENV_MOCK, FUTURE_TRADE), 1.0 - RATE, TOLERANCE_PRICE);
  }
  
  @Test
  public void test_parSpread() {
    double referencePrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(ENV_MOCK, FUTURE_TRADE) - referencePrice;
    double parSpreadComputed = PRICER_TRADE.parSpread(ENV_MOCK, FUTURE_TRADE, referencePrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }

  //------------------------------------------------------------------------- 
  public void test_presentValue() {
    double lastClosingPrice = 1.025;
    IborFuture future = FUTURE_TRADE.getSecurity().getProduct();
    IborFutureTradePricerFn pricerFn = DefaultIborFutureTradePricerFn.DEFAULT;
    double expected = ((1.0 - RATE) - lastClosingPrice) *
        future.getAccrualFactor() * future.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(ENV_MOCK, FUTURE_TRADE, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
    assertEquals(computed.getCurrency(), future.getCurrency());
  }

  //-------------------------------------------------------------------------   
  @Test
  public void test_parSpreadSensitivity() {
    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(ENV_MOCK, FUTURE_PRODUCT);
    PointSensitivities sensiComputed = PRICER_TRADE.parSpreadSensitivity(ENV_MOCK, FUTURE_TRADE);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA));
  }

  //-------------------------------------------------------------------------   
  @Test
  public void test_presentValueSensitivity() {
    PointSensitivities sensiPrice = PRICER_PRODUCT.priceSensitivity(ENV_MOCK, FUTURE_PRODUCT);
    PointSensitivities sensiPresentValueExpected = sensiPrice.multipliedBy(
        FUTURE_PRODUCT.getNotional() * FUTURE_PRODUCT.getAccrualFactor() * FUTURE_TRADE.getQuantity());
    PointSensitivities sensiPresentValueComputed = PRICER_TRADE.presentValueSensitivity(ENV_MOCK, FUTURE_TRADE);
    assertTrue(sensiPresentValueComputed.equalWithTolerance(sensiPresentValueExpected, TOLERANCE_PV_DELTA));
    
  }

}
