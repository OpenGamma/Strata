package com.opengamma.platform.pricer.impl.futures;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.analytics.financial.provider.description.interestrate.NormalSTIRFuturesProviderInterface;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.ExpandedIborFutureOption;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.finance.future.IborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureOptionProductPricerFn;
import com.opengamma.platform.pricer.impl.future.NormalExpandedIborFutureOptionPricerFn;

/**
 * Test NormalExpandedIborFutureOptionPricerFn.
 */
@Test
public class NormalExpandedIborFutureOptionPricerFnTest {
  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);
  private final NormalSTIRFuturesProviderInterface mockVolSurface = mock(NormalSTIRFuturesProviderInterface.class);

  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();

  private static final double TOL = 1.0e-14;

  /**
   * Test price. 
   */
  public void priceTest() {
    IborFutureOption option = IborFuturesDummyData.IBOR_FUTURE_OPTION;
    IborFuture underlying = option.getIborFuture();
    double rate = 0.045;
    double timeToExpiry = 0.25;
    double timeToFixing = 0.35;
    double volatility = 0.62;
    when(mockEnv.iborIndexRate(underlying.getIndex(), underlying.getLastTradeDate())).thenReturn(rate);
    double strike = option.getStrikePrice();
    boolean isCall = option.isIsCall();
    when(mockEnv.relativeTime(option.getExpirationDate())).thenReturn(timeToExpiry);
    when(mockEnv.relativeTime(underlying.getLastTradeDate())).thenReturn(timeToFixing);
    when(mockVolSurface.getVolatility(timeToExpiry, timeToFixing - timeToExpiry, strike, 1.0 - rate)).thenReturn(
        volatility);
    EuropeanVanillaOption vanillaOption = new EuropeanVanillaOption(strike, timeToExpiry, isCall);
    NormalFunctionData normalPoint = new NormalFunctionData(1.0 - rate, 1.0, volatility);
    double priceExpected = NORMAL_FUNCTION.getPriceFunction(vanillaOption).evaluate(normalPoint);
    IborFutureOptionProductPricerFn<ExpandedIborFutureOption> pricer = NormalExpandedIborFutureOptionPricerFn.DEFAULT;
    double priceComputed = pricer.price(mockEnv, option.expand(), mockVolSurface);
    assertEquals(priceComputed, priceExpected, TOL);
  }

  /**
   * Test present value. 
   */
  public void presentValueTest() {
    IborFutureOption option = IborFuturesDummyData.IBOR_FUTURE_OPTION;
    IborFuture underlying = option.getIborFuture();
    IborFutureOptionSecurityTrade optionTrade = IborFuturesDummyData.IBOR_FUTURE_OPTION_SECURITY_TRADE;
    double lastClosingPrice = 0.070;
    double rate = 0.045;
    double timeToExpiry = 0.25;
    double timeToFixing = 0.35;
    double volatility = 0.62;
    double strike = option.getStrikePrice();
    when(mockEnv.iborIndexRate(underlying.getIndex(), underlying.getLastTradeDate())).thenReturn(rate);
    when(mockEnv.relativeTime(option.getExpirationDate())).thenReturn(timeToExpiry);
    when(mockEnv.relativeTime(underlying.getLastTradeDate())).thenReturn(timeToFixing);
    when(mockVolSurface.getVolatility(timeToExpiry, timeToFixing - timeToExpiry, strike, 1.0 - rate)).thenReturn(
        volatility);
    NormalExpandedIborFutureOptionPricerFn pricer = new NormalExpandedIborFutureOptionPricerFn();
    double price = pricer.price(mockEnv, option.expand(), mockVolSurface);
    double pvExpected = (price - lastClosingPrice) * optionTrade.getMultiplier() * underlying.getNotional() *
        underlying.getAccrualFactor();
    CurrencyAmount pvComputed = pricer.presentValue(mockEnv, option.expand(), optionTrade, lastClosingPrice,
        mockVolSurface);
    assertEquals(pvComputed.getCurrency(), underlying.getCurrency());
    assertEquals(pvComputed.getAmount(), pvExpected, TOL);
  }
}
