/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentEventPricer;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.SwapPaymentEvent;

/**
 * Test.
 */
@Test
public class DispatchingSwapPaymentEventPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider();
  private static final SwapPaymentEventPricer<NotionalExchange> MOCK_NOTIONAL_EXG = mock(SwapPaymentEventPricer.class);
  private static final SwapPaymentEventPricer<FxResetNotionalExchange> MOCK_FX_NOTIONAL_EXG =
      mock(SwapPaymentEventPricer.class);

  //-------------------------------------------------------------------------
  public void test_presentValue_NotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<NotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.presentValue(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.presentValue(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV), expected, 0d);
  }

  public void test_presentValue_FxResetNotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<FxResetNotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.presentValue(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.presentValue(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV), expected, 0d);
  }

  public void test_presentValue_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockPaymentEvent, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_forecastValue_NotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<NotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.forecastValue(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.forecastValue(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV), expected, 0d);
  }

  public void test_forecastValue_FxResetNotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<FxResetNotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.forecastValue(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.forecastValue(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV), expected, 0d);
  }

  public void test_forecastValue_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.forecastValue(mockPaymentEvent, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValueSensitivity(mockPaymentEvent, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_forecastValueSensitivity_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.forecastValueSensitivity(mockPaymentEvent, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure_NotionalExchange() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    SwapPaymentEventPricer<NotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.currencyExposure(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.currencyExposure(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV), expected);
  }

  public void test_currencyExposure_FxResetNotionalExchange() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    SwapPaymentEventPricer<FxResetNotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.currencyExposure(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.currencyExposure(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV), expected);
  }

  public void test_currencyExposure_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.currencyExposure(mockPaymentEvent, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_currentCash_NotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<NotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.currentCash(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.currentCash(SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV), expected, 0d);
  }

  public void test_currentCash_FxResetNotionalExchange() {
    double expected = 0.0123d;
    SwapPaymentEventPricer<FxResetNotionalExchange> mockCalledFn = mock(SwapPaymentEventPricer.class);
    when(mockCalledFn.currentCash(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentEventPricer test = new DispatchingSwapPaymentEventPricer(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.currentCash(SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD, MOCK_PROV), expected, 0d);
  }

  public void test_currentCash_unknownType() {
    SwapPaymentEvent mockPaymentEvent = mock(SwapPaymentEvent.class);
    DispatchingSwapPaymentEventPricer test = DispatchingSwapPaymentEventPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.currentCash(mockPaymentEvent, MOCK_PROV));
  }
}
