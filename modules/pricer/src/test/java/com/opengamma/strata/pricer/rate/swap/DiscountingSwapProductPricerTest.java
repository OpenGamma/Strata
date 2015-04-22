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
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
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
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Tests {@link DiscountingSwapProductPricer}.
 */
@Test
public class DiscountingSwapProductPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider(date(2014, 1, 22));
  private static final double TOLERANCE = 1.0e-12;

  //-------------------------------------------------------------------------
  public void test_parRate_singleCurrency() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.discountFactor(GBP, FIXED_RATE_PAYMENT_PERIOD_PAY.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockProv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockProv.fxRate(GBP, GBP)).thenReturn(1.0);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIbor = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, mockProv))
        .thenReturn(pvCpnIbor);
    double pvCpnFixed = -0.99 * 0.0123d * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY, mockProv))
        .thenReturn(pvCpnFixed);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotional = 980_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv))
        .thenReturn(pvNotional);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_GBP, mockProv))
        .thenReturn(-pvNotional);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    double pvbp = pricerLeg.pvbp(FIXED_EXPANDED_SWAP_LEG_PAY, mockProv);
    double parRateExpected1 = -(pvCpnIbor + -pvNotional + pvNotional) / pvbp;
    double parRateExpected2 = fwdRate;
    double parRateComputed = pricerSwap.parRate(expanded, mockProv);
    assertEquals(parRateComputed, parRateExpected1, TOLERANCE);
    assertEquals(parRateComputed, parRateExpected2, TOLERANCE);
  }

  public void test_parRate_crossCurrency() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockProv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockProv.fxRate(GBP, GBP)).thenReturn(1.0);
    when(mockProv.fxRate(USD, USD)).thenReturn(1.0);
    double fxGbpUsd = 1.51d;
    when(mockProv.fxRate(GBP, USD)).thenReturn(fxGbpUsd);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIborGbp = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, mockProv))
        .thenReturn(pvCpnIborGbp);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotionalGbp = 980_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv))
        .thenReturn(pvNotionalGbp);
    double pvNotionalUsd = -fxGbpUsd * 981_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_USD, mockProv))
        .thenReturn(pvNotionalUsd);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    double pvbp = pricerLeg.pvbp(FIXED_EXPANDED_SWAP_LEG_PAY_USD, mockProv);
    double parRateExpected = -((pvCpnIborGbp + pvNotionalGbp) * fxGbpUsd + pvNotionalUsd) / pvbp;
    double parRateComputed = pricerSwap.parRate(expanded, mockProv);
    assertEquals(parRateComputed, parRateExpected, TOLERANCE);
  }

  public void test_parRate_bothLegFloating() {
    Swap swap = Swap.builder()
        .legs(IBOR_EXPANDED_SWAP_LEG_REC, IBOR_EXPANDED_SWAP_LEG_REC)
        .build();
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    assertThrowsIllegalArg(() -> pricerSwap.parRate(swap, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(35d);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_GBP, MOCK_PROV))
        .thenReturn(-30d);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(pricerSwap.presentValue(expanded, MOCK_PROV), MultiCurrencyAmount.of(GBP, 505d));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE, MOCK_PROV),
        pricerSwap.presentValue(expanded, MOCK_PROV));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.presentValue(expanded, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE_CROSS_CURRENCY, MOCK_PROV),
        pricerSwap.presentValue(expanded, MOCK_PROV));
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockRatesProvider.RATE - 500d);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.presentValue(expanded, USD, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE_CROSS_CURRENCY, USD, MOCK_PROV),
        pricerSwap.presentValue(expanded, USD, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(FIXED_RATE_PAYMENT_PERIOD_PAY, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(pricerSwap.futureValue(expanded, MOCK_PROV), MultiCurrencyAmount.of(GBP, 500d));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValue(SWAP_TRADE, MOCK_PROV),
        pricerSwap.futureValue(expanded, MOCK_PROV));
  }

  public void test_futureValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.futureValue(expanded, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValue(SWAP_TRADE_CROSS_CURRENCY, MOCK_PROV),
        pricerSwap.futureValue(expanded, MOCK_PROV));
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
    when(mockPeriod.presentValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.presentValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.presentValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.presentValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    PointSensitivities res = pricerSwap.presentValueSensitivity(SWAP, MOCK_PROV).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValueSensitivity(SWAP_TRADE, MOCK_PROV),
        pricerSwap.presentValueSensitivity(SWAP, MOCK_PROV).build());
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
    when(mockPeriod.futureValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.futureValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.futureValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.futureValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    PointSensitivities res = pricerSwap.futureValueSensitivity(SWAP, MOCK_PROV).build();

    CurveSensitivityTestUtil.assertMulticurveSensitivity(res, expected, TOLERANCE);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValueSensitivity(SWAP_TRADE, MOCK_PROV),
        pricerSwap.futureValueSensitivity(SWAP, MOCK_PROV).build());
  }

}
