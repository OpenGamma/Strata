/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.ignoreThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.pricer.swap.SwapPaymentPeriodPricer;
import com.opengamma.strata.product.swap.KnownAmountSwapPaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Test.
 */
public class DispatchingSwapPaymentPeriodPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider();
  private static final SwapPaymentPeriodPricer<RatePaymentPeriod> MOCK_RATE = mock(SwapPaymentPeriodPricer.class);
  private static final SwapPaymentPeriodPricer<KnownAmountSwapPaymentPeriod> MOCK_KNOWN = mock(SwapPaymentPeriodPricer.class);

  @Test
  public void test_presentValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    SwapPaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(SwapPaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentPeriodPricer test = new DispatchingSwapPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertThat(test.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV)).isCloseTo(expected, offset(0d));
  }

  @Test
  public void test_presentValue_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.presentValue(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    SwapPaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(SwapPaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.forecastValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentPeriodPricer test = new DispatchingSwapPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertThat(test.forecastValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV)).isCloseTo(expected, offset(0d));
  }

  @Test
  public void test_forecastValue_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.forecastValue(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.presentValueSensitivity(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValueSensitivity_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.forecastValueSensitivity(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure_RatePaymentPeriod() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 0.0123d);
    SwapPaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(SwapPaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.currencyExposure(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentPeriodPricer test = new DispatchingSwapPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertThat(test.currencyExposure(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV)).isEqualTo(expected);
  }

  @Test
  public void test_currencyExposure_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.currencyExposure(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currentCash_RatePaymentPeriod() {
    double expected = 0.0123d;
    SwapPaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(SwapPaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.currentCash(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingSwapPaymentPeriodPricer test = new DispatchingSwapPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertThat(test.currentCash(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV)).isCloseTo(expected, offset(0d));
  }

  @Test
  public void test_currentCash_unknownType() {
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);
    DispatchingSwapPaymentPeriodPricer test = DispatchingSwapPaymentPeriodPricer.DEFAULT;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.currentCash(mockPaymentPeriod, MOCK_PROV));
  }

  //------------------------------------------------------------------------- 
  @Test
  public void coverage() {
    DispatchingSwapPaymentPeriodPricer test = new DispatchingSwapPaymentPeriodPricer(
        MOCK_RATE,
        MOCK_KNOWN);

    SwapPaymentPeriod kapp = KnownAmountSwapPaymentPeriod.builder()
        .payment(Payment.of(CurrencyAmount.of(GBP, 1000), date(2015, 8, 21)))
        .startDate(date(2015, 5, 19))
        .endDate(date(2015, 8, 19))
        .build();
    SwapPaymentPeriod mockPaymentPeriod = mock(SwapPaymentPeriod.class);

    ignoreThrows(() -> test.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.presentValue(kapp, MOCK_PROV));
    ignoreThrows(() -> test.presentValue(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.forecastValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.forecastValue(kapp, MOCK_PROV));
    ignoreThrows(() -> test.forecastValue(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.pvbp(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.pvbp(kapp, MOCK_PROV));
    ignoreThrows(() -> test.pvbp(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.presentValueSensitivity(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.presentValueSensitivity(kapp, MOCK_PROV));
    ignoreThrows(() -> test.presentValueSensitivity(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.forecastValueSensitivity(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.forecastValueSensitivity(kapp, MOCK_PROV));
    ignoreThrows(() -> test.forecastValueSensitivity(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.pvbpSensitivity(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.pvbpSensitivity(kapp, MOCK_PROV));
    ignoreThrows(() -> test.pvbpSensitivity(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.accruedInterest(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.accruedInterest(kapp, MOCK_PROV));
    ignoreThrows(() -> test.accruedInterest(mockPaymentPeriod, MOCK_PROV));

    ExplainMapBuilder explain = ExplainMap.builder();
    ignoreThrows(() -> test.explainPresentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainPresentValue(kapp, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainPresentValue(mockPaymentPeriod, MOCK_PROV, explain));

    ignoreThrows(() -> test.currencyExposure(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.currencyExposure(kapp, MOCK_PROV));
    ignoreThrows(() -> test.currencyExposure(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.currentCash(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.currentCash(kapp, MOCK_PROV));
    ignoreThrows(() -> test.currentCash(mockPaymentPeriod, MOCK_PROV));
  }

}
