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
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_CROSS_CURRENCY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE_CROSS_CURRENCY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
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
public class DiscountingSwapProductPricerTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final double TOLERANCE = 1.0e-12;

  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(35d);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded), MultiCurrencyAmount.of(GBP, 570d));

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE), test.presentValue(MOCK_ENV, expanded));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.presentValue(MOCK_ENV, expanded));
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockPricingEnvironment.RATE - 500d);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded, USD), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.presentValue(MOCK_ENV, expanded));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(test.futureValue(MOCK_ENV, expanded), MultiCurrencyAmount.of(GBP, 500d));

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.futureValue(MOCK_ENV, SWAP_TRADE), test.futureValue(MOCK_ENV, expanded));
  }

  public void test_futureValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.futureValue(MOCK_ENV, expanded), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.futureValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.futureValue(MOCK_ENV, expanded));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    // ibor leg
    IborRateSensitivity fwdSense =
        IborRateSensitivity.of(GBP_LIBOR_3M, GBP, IBOR_RATE_OBSERVATION.getFixingDate(), 140.0);
    ZeroRateSensitivity dscSense =
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD.getPaymentDate(), -162.0);
    PointSensitivityBuilder sensiFloating = fwdSense.combinedWith(dscSense);
    // fixed leg
    PointSensitivityBuilder sensiFixed =
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD.getPaymentDate(), 152.0);
    // events
    Currency ccy = IBOR_EXPANDED_SWAP_LEG.getCurrency();
    LocalDate paymentDateEvent = NOTIONAL_EXCHANGE.getPaymentDate();
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, paymentDateEvent, -134.0);
    PointSensitivities expected = sensiFloating.build()
        .combinedWith(sensiEvent.build())
        .combinedWith(sensiFixed.build())
        .combinedWith(sensiEvent.build());

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG.getPaymentPeriods().get(0)))
        .thenReturn(sensiFloating);
    when(mockPeriod.presentValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG.getPaymentPeriods().get(0)))
        .thenReturn(sensiFixed);
    when(mockEvent.presentValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG.getPaymentEvents().get(0)))
        .thenReturn(sensiEvent);
    when(mockEvent.presentValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG.getPaymentEvents().get(0)))
        .thenReturn(sensiEvent);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    PointSensitivities res = test.presentValueSensitivity(MOCK_ENV, expanded).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValueSensitivity(MOCK_ENV, SWAP_TRADE), test.presentValueSensitivity(MOCK_ENV, expanded));
  }

  public void test_futureValueSensitivity() {
    // ibor leg
    PointSensitivityBuilder sensiFloating =
        IborRateSensitivity.of(GBP_LIBOR_3M, GBP, IBOR_RATE_OBSERVATION.getFixingDate(), 140.0);
    // fixed leg
    PointSensitivityBuilder sensiFixed = PointSensitivityBuilder.none();
    // events
    PointSensitivityBuilder sensiEvent = PointSensitivityBuilder.none();
    PointSensitivities expected = sensiFloating.build();

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.futureValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG.getPaymentPeriods().get(0)))
        .thenReturn(sensiFloating);
    when(mockPeriod.futureValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG.getPaymentPeriods().get(0)))
        .thenReturn(sensiFixed);
    when(mockEvent.futureValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG.getPaymentEvents().get(0)))
        .thenReturn(sensiEvent);
    when(mockEvent.futureValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG.getPaymentEvents().get(0)))
        .thenReturn(sensiEvent);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    PointSensitivities res = test.futureValueSensitivity(MOCK_ENV, expanded).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.futureValueSensitivity(MOCK_ENV, SWAP_TRADE), test.futureValueSensitivity(MOCK_ENV, expanded));
  }

}
