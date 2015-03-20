/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE;
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
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test.
 */
@Test
public class DiscountingSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final PricingEnvironment MOCK_ENV_FUTURE = new MockPricingEnvironment(date(2040, 1, 22));
  private static final double TOLERANCE = 1.0e-12;

  public void test_presentValue_withCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  public void test_presentValue_withCurrency_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.presentValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.futureValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.futureValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.futureValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = IBOR_RATE_OBSERVATION.getFixingDate();
    LocalDate paymentDate = IBOR_RATE_PAYMENT_PERIOD.getPaymentDate();

    IborRateSensitivity fwdSense = IborRateSensitivity.of(index, ccy, fixingDate, 140.0);
    ZeroRateSensitivity dscSense = ZeroRateSensitivity.of(ccy, paymentDate, -162.0);
    PointSensitivityBuilder sensiPeriod = fwdSense.combinedWith(dscSense);
    LocalDate paymentDateEvent = NOTIONAL_EXCHANGE.getPaymentDate();
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, paymentDateEvent, -134.0);
    PointSensitivities expected = sensiPeriod.build().combinedWith(sensiEvent.build());

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(MOCK_ENV, expSwapLeg.getPaymentPeriods().get(0)))
        .thenReturn(sensiPeriod);
    when(mockEvent.presentValueSensitivity(MOCK_ENV, expSwapLeg.getPaymentEvents().get(0)))
        .thenReturn(sensiEvent);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.presentValueSensitivity(MOCK_ENV, expSwapLeg).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);
  }

  public void test_futureValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = IBOR_RATE_OBSERVATION.getFixingDate();
    PointSensitivityBuilder sensiPeriod = IborRateSensitivity.of(index, ccy, fixingDate, 140.0);
    PointSensitivities expected = sensiPeriod.build();

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.futureValueSensitivity(MOCK_ENV, expSwapLeg.getPaymentPeriods().get(0)))
        .thenReturn(sensiPeriod);
    when(mockEvent.futureValueSensitivity(MOCK_ENV, expSwapLeg.getPaymentEvents().get(0)))
        .thenReturn(PointSensitivityBuilder.none());
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.futureValueSensitivity(MOCK_ENV, expSwapLeg).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);
  }

}
