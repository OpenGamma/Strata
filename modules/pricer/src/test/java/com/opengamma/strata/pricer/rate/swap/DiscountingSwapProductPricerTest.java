/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_USD;
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
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.pricer.CurveSensitivityTestUtil;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Tests {@link DiscountingSwapProductPricer}.
 */
@Test
public class DiscountingSwapProductPricerTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final double TOLERANCE = 1.0e-12;

  //-------------------------------------------------------------------------
  public void test_parRate_singleCurrency() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.discountFactor(GBP, FIXED_RATE_PAYMENT_PERIOD_PAY.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockEnv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockEnv.fxRate(GBP, GBP)).thenReturn(1.0);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIbor = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(mockEnv, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(pvCpnIbor);
    double pvCpnFixed = -0.99 * 0.0123d * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(mockEnv, FIXED_RATE_PAYMENT_PERIOD_PAY))
        .thenReturn(pvCpnFixed);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotional = 980_000d;
    when(mockEvent.presentValue(mockEnv, NOTIONAL_EXCHANGE_REC_GBP))
        .thenReturn(pvNotional);
    when(mockEvent.presentValue(mockEnv, NOTIONAL_EXCHANGE_PAY_GBP))
    .thenReturn(-pvNotional);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    double pvbp = DiscountingSwapLegPricer.pvbp(mockEnv, FIXED_EXPANDED_SWAP_LEG_PAY);
    double parRateExpected1 = -(pvCpnIbor + -pvNotional + pvNotional) / pvbp;
    double parRateExpected2 = fwdRate;
    double parRateComputed = pricerSwap.parRate(mockEnv, expanded);
    assertEquals(parRateComputed, parRateExpected1, TOLERANCE);
    assertEquals(parRateComputed, parRateExpected2, TOLERANCE);
  }

  public void test_parRate_crossCurrency() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockEnv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockEnv.fxRate(GBP, GBP)).thenReturn(1.0);
    when(mockEnv.fxRate(USD, USD)).thenReturn(1.0);
    double fxGbpUsd = 1.51d;
    when(mockEnv.fxRate(GBP, USD)).thenReturn(fxGbpUsd);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIborGbp = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(mockEnv, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(pvCpnIborGbp);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotionalGbp = 980_000d;
    when(mockEvent.presentValue(mockEnv, NOTIONAL_EXCHANGE_REC_GBP))
        .thenReturn(pvNotionalGbp);
    double pvNotionalUsd = -fxGbpUsd*981_000d;
    when(mockEvent.presentValue(mockEnv, NOTIONAL_EXCHANGE_PAY_USD))
    .thenReturn(pvNotionalUsd);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    double pvbp = DiscountingSwapLegPricer.pvbp(mockEnv, FIXED_EXPANDED_SWAP_LEG_PAY_USD);
    double parRateExpected = -((pvCpnIborGbp + pvNotionalGbp) * fxGbpUsd + pvNotionalUsd) / pvbp;
    double parRateComputed = pricerSwap.parRate(mockEnv, expanded);
    assertEquals(parRateComputed, parRateExpected, TOLERANCE);
  }
  
  public void test_parRate_bothLegFloating() {    
    Swap swap = Swap.builder()
      .legs(IBOR_EXPANDED_SWAP_LEG_REC, IBOR_EXPANDED_SWAP_LEG_REC)
      .build();
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    assertThrowsIllegalArg(() -> pricerSwap.parRate(MOCK_ENV, swap));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_PAY))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE_REC_GBP))
        .thenReturn(35d);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE_PAY_GBP))
    .thenReturn(-30d);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded), MultiCurrencyAmount.of(GBP, 505d));

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE), test.presentValue(MOCK_ENV, expanded));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_PAY_USD))
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
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_PAY_USD))
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
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_PAY))
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
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_PAY_USD))
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
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD_REC.getPaymentDate(), -162.0);
    PointSensitivityBuilder sensiFloating = fwdSense.combinedWith(dscSense);
    // fixed leg
    PointSensitivityBuilder sensiFixed =
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD_REC.getPaymentDate(), 152.0);
    // events
    Currency ccy = IBOR_EXPANDED_SWAP_LEG_REC.getCurrency();
    LocalDate paymentDateEvent = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate();
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, paymentDateEvent, -134.0);
    PointSensitivities expected = sensiFloating.build()
        .combinedWith(sensiEvent.build())
        .combinedWith(sensiFixed.build())
        .combinedWith(sensiEvent.build());

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG_REC.getPaymentPeriods().get(0)))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.presentValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0)))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.presentValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG_REC.getPaymentEvents().get(0)))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.presentValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0)))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.presentValueSensitivity(MOCK_ENV, SWAP).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(
        testTrade.presentValueSensitivity(MOCK_ENV, SWAP_TRADE),
        test.presentValueSensitivity(MOCK_ENV, SWAP).build());
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
    when(mockPeriod.futureValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG_REC.getPaymentPeriods().get(0)))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.futureValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0)))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.futureValueSensitivity(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG_REC.getPaymentEvents().get(0)))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.futureValueSensitivity(MOCK_ENV, FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0)))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.futureValueSensitivity(MOCK_ENV, SWAP).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(
        testTrade.futureValueSensitivity(MOCK_ENV, SWAP_TRADE),
        test.futureValueSensitivity(MOCK_ENV, SWAP).build());
  }

}
