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
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Tests {@link DiscountingSwapLegPricer}.
 */
@Test
public class DiscountingSwapLegPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider(date(2014, 1, 22));
  private static final RatesProvider MOCK_PROV_FUTURE = new MockRatesProvider(date(2040, 1, 22));
  
  private static final double TOLERANCE = 1.0e-12;
  private static final double TOLERANCE_DELTA = 1.0E+0;

  private static final DiscountingSwapLegPricer PRICER_LEG = DiscountingSwapLegPricer.DEFAULT;
  private static final ImmutableRatesProvider RATES_GBP = RatesProviderDataSets.MULTI_GBP;
  private static final ImmutableRatesProvider RATES_USD = RatesProviderDataSets.MULTI_USD;
  private static final double FD_SHIFT = 1.0E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR = 
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  public void test_couponEquivalent_TwoPeriods() {
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
    when(mockProv.getValuationDate()).thenReturn(date(2014, 1, 22));
    double pvbp = PRICER_LEG.pvbp(leg, mockProv);
    double ceExpected = PRICER_LEG.presentValuePeriodsInternal(leg, mockProv) / pvbp;
    double ceComputed = PRICER_LEG.couponEquivalent(leg, mockProv, pvbp);
    assertEquals(ceComputed, ceExpected, TOLERANCE);
  }

  //-------------------------------------------------------------------------
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

  public void test_pvbp_FxReset() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(()->test.pvbp(FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbp_Compounding() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(()->test.pvbp(FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_withCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, USD, MOCK_PROV), expected);
  }

  public void test_presentValue_withCurrency_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, USD, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 1500d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV), expected);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV_FUTURE), expected);
  }
  
  public void test_presentValue_events() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.presentValueEventsInternal(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV), 1000d);
  }
  
  public void test_presentValue_periods() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.presentValuePeriodsInternal(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV), 500d);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.futureValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.futureValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV), expected);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.futureValue(IBOR_EXPANDED_SWAP_LEG_REC_GBP, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = IBOR_RATE_OBSERVATION.getFixingDate();
    LocalDate paymentDate = IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getPaymentDate();

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

    assertTrue(res.equalWithTolerance(expected, TOLERANCE));
  }  

  public void test_presentValueSensitivity_finiteDifference() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivity(expSwapLeg, RATES_GBP).build();
    CurveParameterSensitivity psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivity psFd = 
        FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP, (p) -> PRICER_LEG.presentValue(expSwapLeg, p));
    ImmutableMap<SensitivityKey, double[]> mapAd = psAd.getSensitivities();
    ImmutableMap<SensitivityKey, double[]> mapFd = psFd.getSensitivities();
    assertEquals(mapAd.size(), 2); // No Libor 6M sensitivity
    assertEquals(mapFd.size(), 3); // Libor 6M sensitivity equal to 0 in Finite Difference
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_events() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivityEventsInternal(expSwapLeg, RATES_GBP).build();
    CurveParameterSensitivity psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivity psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP, 
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValueEventsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_periods() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivityPeriodsInternal(expSwapLeg, RATES_GBP).build();
    CurveParameterSensitivity psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivity psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP, 
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValuePeriodsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_pvbpSensitivity() {
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    PointSensitivities point = PRICER_LEG.pvbpSensitivity(leg, RATES_USD).build();
    CurveParameterSensitivity pvbpsAd = RATES_USD.parameterSensitivity(point);
    CurveParameterSensitivity pvbpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_USD, 
        (p) -> CurrencyAmount.of(USD, PRICER_LEG.pvbp(leg, p)));
    assertTrue(pvbpsAd.equalWithTolerance(pvbpsFd, TOLERANCE_DELTA));
  }

  public void test_pvbpSensitivity_FxReset() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(()->test.pvbpSensitivity(FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbpSensitivity_Compounding() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(()->test.pvbpSensitivity(FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD, MOCK_PROV));
  }

  public void test_futureValueSensitivity() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
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

    assertTrue(res.equalWithTolerance(expected, TOLERANCE));
  }

}
