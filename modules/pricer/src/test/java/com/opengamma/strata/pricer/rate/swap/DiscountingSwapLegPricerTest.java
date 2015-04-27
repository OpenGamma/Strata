/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.CurveSensitivityTestUtil;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Tests {@link DiscountingSwapLegPricer}.
 */
@Test
public class DiscountingSwapLegPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider(date(2014, 1, 22));
  private static final RatesProvider MOCK_PROV_FUTURE = new MockRatesProvider(date(2040, 1, 22));
  private static final double TOLERANCE = 1.0e-12;

  public void test_pvbp_OnePeriod() {
    RatesProvider mockProv = mock(RatesProvider.class);
    double df = 0.99d;
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(df);
    double expected = df * FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getNotional() *
        FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getAccrualPeriods().get(0).getYearFraction();
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertEquals(test.pvbp(FIXED_EXPANDED_SWAP_LEG_PAY_USD, mockProv), expected, TOLERANCE);
  }

  public void test_pvbp_TwoPeriods() {
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    RatesProvider mockProv = mock(RatesProvider.class);
    double df1 = 0.99d;
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(df1);
    double df2 = 0.98d;
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2.getPaymentDate()))
        .thenReturn(df2);
    double expected = df1 * FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getNotional() *
        FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getAccrualPeriods().get(0).getYearFraction();
    expected += df2 * FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2.getNotional() *
        FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2.getAccrualPeriods().get(0).getYearFraction();
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertEquals(test.pvbp(leg, mockProv), expected, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_withCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC, USD, MOCK_PROV), expected);
  }

  public void test_presentValue_withCurrency_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC, USD, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC, MOCK_PROV), expected);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.futureValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.futureValue(IBOR_EXPANDED_SWAP_LEG_REC, MOCK_PROV), expected);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.futureValue(IBOR_EXPANDED_SWAP_LEG_REC, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = IBOR_RATE_OBSERVATION.getFixingDate();
    LocalDate paymentDate = IBOR_RATE_PAYMENT_PERIOD_REC.getPaymentDate();

    IborRateSensitivity fwdSense = IborRateSensitivity.of(index, ccy, fixingDate, 140.0);
    ZeroRateSensitivity dscSense = ZeroRateSensitivity.of(ccy, paymentDate, -162.0);
    PointSensitivityBuilder sensiPeriod = fwdSense.combinedWith(dscSense);
    LocalDate paymentDateEvent = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate();
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, paymentDateEvent, -134.0);
    PointSensitivities expected = sensiPeriod.build().combinedWith(sensiEvent.build());

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(expSwapLeg.getPaymentPeriods().get(0), MOCK_PROV))
        .thenReturn(sensiPeriod);
    when(mockEvent.presentValueSensitivity(expSwapLeg.getPaymentEvents().get(0), MOCK_PROV))
        .thenReturn(sensiEvent);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.presentValueSensitivity(expSwapLeg, MOCK_PROV).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);
  }

  public void test_futureValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = IBOR_RATE_OBSERVATION.getFixingDate();
    PointSensitivityBuilder sensiPeriod = IborRateSensitivity.of(index, ccy, fixingDate, 140.0);
    PointSensitivities expected = sensiPeriod.build();

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.futureValueSensitivity(expSwapLeg.getPaymentPeriods().get(0), MOCK_PROV))
        .thenReturn(sensiPeriod);
    when(mockEvent.futureValueSensitivity(expSwapLeg.getPaymentEvents().get(0), MOCK_PROV))
        .thenReturn(PointSensitivityBuilder.none());
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.futureValueSensitivity(expSwapLeg, MOCK_PROV).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);
  }

}
