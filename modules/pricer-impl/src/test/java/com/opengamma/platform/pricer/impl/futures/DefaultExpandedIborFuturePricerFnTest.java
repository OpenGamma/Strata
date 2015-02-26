package com.opengamma.platform.pricer.impl.futures;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.finance.future.IborFutureSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;
import com.opengamma.platform.pricer.impl.future.DefaultExpandedIborFuturePricerFn;

/**
 * Test DefaultExpandedIborFuturePricerFn.
 */
@Test
public class DefaultExpandedIborFuturePricerFnTest {

  private static final double TOL = 1.0e-12;
  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  /**
   * test price method. 
   */
  public void priceTest() {
    double rate = 0.045;
    IborFuture iborFuture = IborFuturesDummyData.IBOR_FUTURE;
    when(mockEnv.iborIndexRate(iborFuture.getIndex(), iborFuture.getLastTradeDate())).thenReturn(rate);
    IborFutureProductPricerFn<ExpandedIborFuture> pricerFn = DefaultExpandedIborFuturePricerFn.DEFAULT;
    assertEquals(pricerFn.price(mockEnv, iborFuture.expand()), 1.0 - rate, TOL);
  }

  /**
   * test present value.
   */
  public void presentValueTest() {
    double rate = 0.045;
    double lastClosingPrice = 1.025;
    IborFuture iborFuture = IborFuturesDummyData.IBOR_FUTURE;
    IborFutureSecurityTrade iborFutureSecurityTrade = IborFuturesDummyData.IBOR_FUTURE_SECURITY_TRADE;
    when(mockEnv.iborIndexRate(iborFuture.getIndex(), iborFuture.getLastTradeDate())).thenReturn(rate);
    DefaultExpandedIborFuturePricerFn pricerFn = new DefaultExpandedIborFuturePricerFn();
    double expected = ((1.0 - rate) - lastClosingPrice) * iborFuture.getAccrualFactor() * iborFuture.getNotional() *
        iborFutureSecurityTrade.getMultiplier();
    CurrencyAmount computed = pricerFn.presentValue(mockEnv, iborFuture.expand(), iborFutureSecurityTrade,
        lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOL);
    assertEquals(computed.getCurrency(), iborFuture.getCurrency());
  }
}
