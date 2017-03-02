/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_CMP_NONE_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_FX_RESET_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATE_ACCRUAL_PERIOD;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATE_ACCRUAL_PERIOD_2;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_CMP_FLAT_REC_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_SWAP_LEG_REC_USD;
import static com.opengamma.strata.pricer.swap.SwapDummyData.IBOR_RATE_COMP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2;
import static com.opengamma.strata.pricer.swap.SwapDummyData.IBOR_SWAP_LEG_REC_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.IBOR_SWAP_LEG_REC_GBP_MULTI;
import static com.opengamma.strata.pricer.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.CompoundingMethod.STRAIGHT;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.MONTHLY;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.type.IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.impl.rate.ForwardInflationInterpolatedRateComputationFn;
import com.opengamma.strata.pricer.impl.rate.ForwardInflationMonthlyRateComputationFn;
import com.opengamma.strata.pricer.impl.swap.DispatchingSwapPaymentEventPricer;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.SwapPaymentEvent;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConvention;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConventions;
import com.opengamma.strata.product.swap.type.IborIborSwapConventions;

/**
 * Tests {@link DiscountingSwapLegPricer}.
 */
@Test
public class DiscountingSwapLegPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider(RatesProviderDataSets.VAL_DATE_2014_01_22);
  private static final RatesProvider MOCK_PROV_FUTURE = new MockRatesProvider(date(2040, 1, 22));

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double TOLERANCE = 1.0e-12;
  private static final double TOLERANCE_DELTA = 1.0E+0;
  private static final double TOLERANCE_PVBP_FD = 1.0E-4;

  private static final DiscountingSwapLegPricer PRICER_LEG = DiscountingSwapLegPricer.DEFAULT;
  private static final ImmutableRatesProvider RATES_GBP = RatesProviderDataSets.MULTI_GBP;
  private static final ImmutableRatesProvider RATES_USD = RatesProviderDataSets.MULTI_USD;
  private static final ImmutableRatesProvider RATES_GBP_USD = RatesProviderDataSets.MULTI_GBP_USD;
  private static final double FD_SHIFT = 1.0E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  //-------------------------------------------------------------------------
  public void test_getters() {
    assertEquals(DiscountingSwapLegPricer.DEFAULT.getPeriodPricer(), SwapPaymentPeriodPricer.standard());
    assertEquals(DiscountingSwapLegPricer.DEFAULT.getEventPricer(), SwapPaymentEventPricer.standard());
  }

  //-------------------------------------------------------------------------
  public void test_couponEquivalent_twoPeriods() {
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
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
    when(mockProv.getValuationDate()).thenReturn(RatesProviderDataSets.VAL_DATE_2014_01_22);
    double pvbp = PRICER_LEG.pvbp(leg, mockProv);
    double ceExpected = PRICER_LEG.presentValuePeriodsInternal(leg, mockProv) / pvbp;
    double ceComputed = PRICER_LEG.couponEquivalent(leg, mockProv, pvbp);
    assertEquals(ceComputed, ceExpected, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_pvbp_onePeriod() {
    RatesProvider mockProv = mock(RatesProvider.class);
    double df = 0.99d;
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(df);
    double expected = df * FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getNotional() *
        FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getAccrualPeriods().get(0).getYearFraction();
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertEquals(test.pvbp(FIXED_SWAP_LEG_PAY_USD, mockProv), expected, TOLERANCE);
  }

  public void test_pvbp_twoPeriods() {
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
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

  public void test_pvbp_compounding_flat_fixed() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    SwapPaymentPeriod p = FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP.getPaymentPeriods().get(0);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(RatesProviderDataSets.VAL_DATE_2014_01_22);
    double df1 = 0.99d;
    when(mockProv.discountFactor(GBP, p.getPaymentDate()))
        .thenReturn(df1);
    double spread = 1.0E-6;
    RateAccrualPeriod ap1 = FIXED_RATE_ACCRUAL_PERIOD.toBuilder().spread(spread).build();
    RateAccrualPeriod ap2 = FIXED_RATE_ACCRUAL_PERIOD_2.toBuilder().spread(spread).build();
    RatePaymentPeriod pp = FIXED_RATE_PAYMENT_PERIOD_CMP_FLAT_REC_GBP.toBuilder().accrualPeriods(ap1, ap2).build();
    ResolvedSwapLeg sl = FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP.toBuilder().paymentPeriods(pp).build();
    CurrencyAmount pv0 = PRICER_LEG.presentValue(FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP, mockProv);
    CurrencyAmount pvP = PRICER_LEG.presentValue(sl, mockProv);
    double pvbpExpected = (pvP.getAmount() - pv0.getAmount()) / spread;
    double pvbpComputed = test.pvbp(FIXED_CMP_FLAT_SWAP_LEG_PAY_GBP, mockProv);
    assertEquals(pvbpComputed, pvbpExpected, TOLERANCE_PVBP_FD);
  }

  public void test_pvbp_compounding_flat_ibor() {
    LocalDate tradeDate = RATES_USD.getValuationDate();
    LocalDate effectiveDate = USD_LIBOR_3M_LIBOR_6M.calculateSpotDateFromTradeDate(tradeDate, REF_DATA);
    LocalDate endDate = effectiveDate.plus(TENOR_10Y);
    double spread = 0.0015;
    double shift = 1.0E-6;
    RateCalculationSwapLeg leg0 = IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M.getSpreadLeg()
        .toLeg(effectiveDate, endDate, RECEIVE, NOTIONAL, spread);
    RateCalculationSwapLeg legP = IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M.getSpreadLeg()
        .toLeg(effectiveDate, endDate, RECEIVE, NOTIONAL, spread + shift);
    double parSpread = PRICER_LEG.pvbp(leg0.resolve(REF_DATA), RATES_USD);
    double pv0 = PRICER_LEG.presentValue(leg0.resolve(REF_DATA), RATES_USD).getAmount();
    double pvP = PRICER_LEG.presentValue(legP.resolve(REF_DATA), RATES_USD).getAmount();
    double parSpreadExpected = (pvP - pv0) / shift;
    assertEquals(parSpread, parSpreadExpected, TOLERANCE_PVBP_FD);
  }

  public void test_pvbp_fxReset() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbp(FIXED_FX_RESET_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbp_compounding_none() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbp(FIXED_CMP_NONE_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_withCurrency() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(IBOR_SWAP_LEG_REC_GBP, USD, MOCK_PROV), expected);
  }

  public void test_presentValue_withCurrency_past() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(IBOR_SWAP_LEG_REC_GBP, USD, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 1500d);
    assertEquals(test.presentValue(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV), expected);
  }

  public void test_presentValue_past() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV_FUTURE), expected);
  }

  public void test_presentValue_events() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.presentValueEventsInternal(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV), 1000d);
  }

  public void test_presentValue_periods() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(500d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.presentValuePeriodsInternal(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV), 500d);
  }

  //-------------------------------------------------------------------------
  public void test_forecastValue() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.forecastValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockEvent.forecastValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.forecastValue(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV), expected);
  }

  public void test_forecastValue_past() {
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.forecastValue(IBOR_SWAP_LEG_REC_GBP, MOCK_PROV_FUTURE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_accruedInterest_firstAccrualPeriod() {
    RatesProvider prov = new MockRatesProvider(IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getStartDate().plusDays(7));
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.accruedInterest(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, prov))
        .thenReturn(1000d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 1000d);
    assertEquals(test.accruedInterest(IBOR_SWAP_LEG_REC_GBP, prov), expected);
  }

  public void test_accruedInterest_valDateBeforePeriod() {
    RatesProvider prov = new MockRatesProvider(IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getStartDate());
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.accruedInterest(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, prov))
        .thenReturn(1000d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.accruedInterest(IBOR_SWAP_LEG_REC_GBP, prov), CurrencyAmount.zero(GBP));
  }

  public void test_accruedInterest_valDateAfterPeriod() {
    RatesProvider prov = new MockRatesProvider(IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getEndDate().plusDays(1));
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    when(mockPeriod.accruedInterest(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, prov))
        .thenReturn(1000d);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    assertEquals(test.accruedInterest(IBOR_SWAP_LEG_REC_GBP, prov), CurrencyAmount.zero(GBP));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    Currency ccy = GBP_LIBOR_3M.getCurrency();

    IborRateSensitivity fwdSense = IborRateSensitivity.of(IBOR_RATE_COMP.getObservation(), 140.0);
    ZeroRateSensitivity dscSense = ZeroRateSensitivity.of(ccy, 3d, -162.0);
    PointSensitivityBuilder sensiPeriod = fwdSense.combinedWith(dscSense);
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, 4d, -134.0);
    PointSensitivities expected = sensiPeriod.build().combinedWith(sensiEvent.build());

    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(expSwapLeg.getPaymentPeriods().get(0), MOCK_PROV))
        .thenReturn(sensiPeriod);
    when(mockEvent.presentValueSensitivity(expSwapLeg.getPaymentEvents().get(0), MOCK_PROV))
        .thenReturn(sensiEvent);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.presentValueSensitivity(expSwapLeg, MOCK_PROV).build();

    assertTrue(res.equalWithTolerance(expected, TOLERANCE));
  }

  public void test_presentValueSensitivity_finiteDifference() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivity(expSwapLeg, RATES_GBP).build();
    CurrencyParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurrencyParameterSensitivities psFd =
        FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP, (p) -> PRICER_LEG.presentValue(expSwapLeg, p));
    ImmutableList<CurrencyParameterSensitivity> listAd = psAd.getSensitivities();
    ImmutableList<CurrencyParameterSensitivity> listFd = psFd.getSensitivities();
    assertEquals(listAd.size(), 2); // No Libor 6M sensitivity
    assertEquals(listFd.size(), 3); // Libor 6M sensitivity equal to 0 in Finite Difference
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_events() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivityEventsInternal(expSwapLeg, RATES_GBP).build();
    CurrencyParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurrencyParameterSensitivities psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP,
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValueEventsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_periods() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivityPeriodsInternal(expSwapLeg, RATES_GBP).build();
    CurrencyParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurrencyParameterSensitivities psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP,
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValuePeriodsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_pvbpSensitivity() {
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    PointSensitivities point = PRICER_LEG.pvbpSensitivity(leg, RATES_USD).build();
    CurrencyParameterSensitivities pvbpsAd = RATES_USD.parameterSensitivity(point);
    CurrencyParameterSensitivities pvbpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_USD,
        (p) -> CurrencyAmount.of(USD, PRICER_LEG.pvbp(leg, p)));
    assertTrue(pvbpsAd.equalWithTolerance(pvbpsFd, TOLERANCE_DELTA));
  }

  public void test_pvbpSensitivity_FxReset() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbpSensitivity(FIXED_FX_RESET_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbpSensitivity_Compounding() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbpSensitivity(FIXED_CMP_NONE_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbpSensitivity_compounding_flat_ibor() {
    LocalDate tradeDate = RATES_USD.getValuationDate();
    LocalDate effectiveDate = USD_LIBOR_3M_LIBOR_6M.calculateSpotDateFromTradeDate(tradeDate, REF_DATA);
    LocalDate endDate = effectiveDate.plus(TENOR_10Y);
    double spread = 0.0015;
    RateCalculationSwapLeg leg = IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M.getSpreadLeg()
        .toLeg(effectiveDate, endDate, RECEIVE, NOTIONAL, spread);
    PointSensitivities pvbppts = PRICER_LEG.pvbpSensitivity(leg.resolve(REF_DATA), RATES_USD).build();
    CurrencyParameterSensitivities psAd = RATES_USD.parameterSensitivity(pvbppts);
    CurrencyParameterSensitivities psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATES_USD,
        (p) -> CurrencyAmount.of(USD, PRICER_LEG.pvbp(leg.resolve(REF_DATA), p)));
    ImmutableList<CurrencyParameterSensitivity> listAd = psAd.getSensitivities();
    ImmutableList<CurrencyParameterSensitivity> listFd = psFd.getSensitivities();
    assertEquals(listAd.size(), 2); // No Libor 6M sensitivity
    assertEquals(listFd.size(), 3); // Libor 6M sensitivity equal to 0 in Finite Difference
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_forecastValueSensitivity() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    PointSensitivityBuilder sensiPeriod = IborRateSensitivity.of(IBOR_RATE_COMP.getObservation(), 140.0);
    PointSensitivities expected = sensiPeriod.build();

    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    when(mockPeriod.forecastValueSensitivity(expSwapLeg.getPaymentPeriods().get(0), MOCK_PROV))
        .thenReturn(sensiPeriod);
    when(mockEvent.forecastValueSensitivity(expSwapLeg.getPaymentEvents().get(0), MOCK_PROV))
        .thenReturn(PointSensitivityBuilder.none());
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    PointSensitivities res = test.forecastValueSensitivity(expSwapLeg, MOCK_PROV).build();

    assertTrue(res.equalWithTolerance(expected, TOLERANCE));
  }

  //-------------------------------------------------------------------------
  public void test_annuityCash_onePeriod() {
    double yield = 0.01;
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    double computed = test.annuityCash(FIXED_SWAP_LEG_REC_USD, yield);
    double expected = SwapDummyData.NOTIONAL * (1d - 1d / (1d + yield / 4d)) / yield;
    assertEquals(computed, expected, SwapDummyData.NOTIONAL * TOLERANCE);
  }

  public void test_annuityCash_twoPeriods() {
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    double yield = 0.01;
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    double computed = test.annuityCash(leg, yield);
    double expected = SwapDummyData.NOTIONAL * (1d - Math.pow(1d + yield / 4d, -2)) / yield;
    assertEquals(computed, expected, SwapDummyData.NOTIONAL * TOLERANCE);
  }

  public void test_annuityCashDerivative_onePeriod() {
    double yield = 0.01;
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    double computed = test.annuityCashDerivative(FIXED_SWAP_LEG_REC_USD, yield).getDerivative(0);
    double expected = 0.5 * (test.annuityCash(FIXED_SWAP_LEG_REC_USD, yield + FD_SHIFT)
        - test.annuityCash(FIXED_SWAP_LEG_REC_USD, yield - FD_SHIFT)) / FD_SHIFT;
    assertEquals(computed, expected, SwapDummyData.NOTIONAL * FD_SHIFT);
  }

  public void test_annuityCashDerivative_twoPeriods() {
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    double yield = 0.01;
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    double computed = test.annuityCashDerivative(leg, yield).getDerivative(0);
    double expected = 0.5 / FD_SHIFT
        * (test.annuityCash(leg, yield + FD_SHIFT) - test.annuityCash(leg, yield - FD_SHIFT));
    assertEquals(computed, expected, SwapDummyData.NOTIONAL * FD_SHIFT);
  }

  //-------------------------------------------------------------------------
  private static final LocalDate DATE_14_06_09 = date(2014, 6, 9);
  private static final LocalDate DATE_19_06_09 = date(2019, 6, 9);
  private static final LocalDate DATE_14_03_31 = date(2014, 3, 31);
  private static final double START_INDEX = 218.0;
  private static final double NOTIONAL = 1000d;
  private static final LocalDate VAL_DATE_INFLATION = date(2014, 7, 8);
  private static final ImmutableRatesProvider RATES_GBP_INFLATION = RatesProviderDataSets.multiGbp(VAL_DATE_INFLATION);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final double CONSTANT_INDEX = 242.0;
  private static final Curve GBPRI_CURVE_FLAT = InterpolatedNodalCurve.of(
      Curves.prices("GB_RPI_CURVE"),
      DoubleArray.of(1, 200),
      DoubleArray.of(CONSTANT_INDEX, CONSTANT_INDEX),
      INTERPOLATOR);

  private static final CurveInterpolator INTERP_SPLINE = CurveInterpolators.NATURAL_CUBIC_SPLINE;
  private static final Curve GBPRI_CURVE = InterpolatedNodalCurve.of(
      Curves.prices("GB_RPI_CURVE"),
      DoubleArray.of(6, 12, 24, 60, 120),
      DoubleArray.of(227.2, 252.6, 289.5, 323.1, 351.1),
      INTERP_SPLINE);

  private static final double EPS = 1.0e-14;

  public void test_inflation_monthly() {
    // setup
    ResolvedSwapLeg swapLeg = createInflationSwapLeg(false, PAY).resolve(REF_DATA);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    Map<Currency, Curve> dscCurve = RATES_GBP_INFLATION.getDiscountCurves();
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(DATE_14_03_31, START_INDEX);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE_INFLATION)
        .discountCurves(dscCurve)
        .priceIndexCurve(GB_RPI, GBPRI_CURVE_FLAT)
        .timeSeries(GB_RPI, ts)
        .build();
    // test forecastValue and presentValue
    CurrencyAmount fvComputed = pricer.forecastValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    double fvExpected = (CONSTANT_INDEX / START_INDEX - 1.0) * (-NOTIONAL);
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = dscFactor * fvExpected;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test forecastValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.forecastValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    ForwardInflationMonthlyRateComputationFn obsFn = ForwardInflationMonthlyRateComputationFn.DEFAULT;
    RatePaymentPeriod paymentPeriod = (RatePaymentPeriod) swapLeg.getPaymentPeriods().get(0);
    InflationMonthlyRateComputation obs =
        (InflationMonthlyRateComputation) paymentPeriod.getAccrualPeriods().get(0).getRateComputation();
    PointSensitivityBuilder pvSensiExpected = obsFn.rateSensitivity(obs, DATE_14_06_09, DATE_19_06_09, prov);
    pvSensiExpected = pvSensiExpected.multipliedBy(-NOTIONAL);
    assertTrue(fvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
    pvSensiExpected = pvSensiExpected.multipliedBy(dscFactor);
    PointSensitivityBuilder dscSensiExpected = prov.discountFactors(GBP).zeroRatePointSensitivity(paymentDate);
    dscSensiExpected = dscSensiExpected.multipliedBy(fvExpected);
    pvSensiExpected = pvSensiExpected.combinedWith(dscSensiExpected);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
  }

  public void test_inflation_interpolated() {
    // setup
    ResolvedSwapLeg swapLeg = createInflationSwapLeg(true, RECEIVE).resolve(REF_DATA);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    Map<Currency, Curve> dscCurve = RATES_GBP_INFLATION.getDiscountCurves();
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(DATE_14_03_31, START_INDEX);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE_INFLATION)
        .discountCurves(dscCurve)
        .priceIndexCurve(GB_RPI, GBPRI_CURVE)
        .timeSeries(GB_RPI, ts)
        .build();
    // test forecastValue and presentValue
    CurrencyAmount fvComputed = pricer.forecastValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    ForwardInflationInterpolatedRateComputationFn obsFn = ForwardInflationInterpolatedRateComputationFn.DEFAULT;
    RatePaymentPeriod paymentPeriod = (RatePaymentPeriod) swapLeg.getPaymentPeriods().get(0);
    InflationInterpolatedRateComputation obs =
        (InflationInterpolatedRateComputation) paymentPeriod.getAccrualPeriods().get(0).getRateComputation();
    double indexRate = obsFn.rate(obs, DATE_14_06_09, DATE_19_06_09, prov);
    double fvExpected = indexRate * (NOTIONAL);
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = dscFactor * fvExpected;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test forecastValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.forecastValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiExpected = obsFn.rateSensitivity(obs, DATE_14_06_09, DATE_19_06_09, prov);
    pvSensiExpected = pvSensiExpected.multipliedBy(NOTIONAL);
    assertTrue(fvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
    pvSensiExpected = pvSensiExpected.multipliedBy(dscFactor);
    PointSensitivityBuilder dscSensiExpected = prov.discountFactors(GBP).zeroRatePointSensitivity(paymentDate);
    dscSensiExpected = dscSensiExpected.multipliedBy(fvExpected);
    pvSensiExpected = pvSensiExpected.combinedWith(dscSensiExpected);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
  }

  private SwapLeg createInflationSwapLeg(boolean interpolated, PayReceive pay) {
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(Frequency.ofYears(5))
        .businessDayAdjustment(adj)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    InflationRateCalculation rateCalc = InflationRateCalculation.builder()
        .index(GB_RPI)
        .indexCalculationMethod(interpolated ? INTERPOLATED : MONTHLY)
        .lag(Period.ofMonths(3))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, NOTIONAL);
    SwapLeg swapLeg = RateCalculationSwapLeg.builder()
        .payReceive(pay)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    return swapLeg;
  }

  public void test_inflation_fixed() {
    // setup
    double fixedRate = 0.05;
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(FOLLOWING, GBLO);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_14_06_09)
        .endDate(DATE_19_06_09)
        .frequency(P12M)
        .businessDayAdjustment(adj)
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.ofYears(5))
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .compoundingMethod(STRAIGHT)
        .build();
    FixedRateCalculation rateCalc = FixedRateCalculation.builder()
        .rate(ValueSchedule.of(fixedRate))
        .dayCount(ONE_ONE) // year fraction is always 1.
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    ResolvedSwapLeg swapLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build()
        .resolve(REF_DATA);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    Map<Currency, Curve> dscCurve = RATES_GBP_INFLATION.getDiscountCurves();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE_INFLATION)
        .discountCurves(dscCurve)
        .build();
    // test forecastValue and presentValue
    CurrencyAmount fvComputed = pricer.forecastValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    double fvExpected = (Math.pow(1.0 + fixedRate, 5) - 1.0) * NOTIONAL;
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = fvExpected * dscFactor;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test forecastValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.forecastValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    assertEquals(fvSensiComputed, PointSensitivityBuilder.none());
    PointSensitivityBuilder pvSensiExpected = prov.discountFactors(GBP).zeroRatePointSensitivity(paymentDate);
    pvSensiExpected = pvSensiExpected.multipliedBy(fvExpected);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_cashFlows() {
    RatesProvider mockProv = mock(RatesProvider.class);
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    DispatchingSwapPaymentEventPricer eventPricer = DispatchingSwapPaymentEventPricer.DEFAULT;
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP_MULTI;
    SwapPaymentPeriod period1 = IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
    SwapPaymentPeriod period2 = IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2;
    NotionalExchange event = NOTIONAL_EXCHANGE_REC_GBP;
    double fv1 = 520d;
    double fv2 = 450d;
    double df = 1.0d;
    double df1 = 0.98;
    double df2 = 0.93;
    when(mockPeriod.forecastValue(period1, mockProv)).thenReturn(fv1);
    when(mockPeriod.forecastValue(period2, mockProv)).thenReturn(fv2);
    when(mockProv.getValuationDate()).thenReturn(LocalDate.of(2014, 7, 1));
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), period1.getPaymentDate())).thenReturn(df1);
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), period2.getPaymentDate())).thenReturn(df2);
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), event.getPaymentDate())).thenReturn(df);
    DiscountingSwapLegPricer pricer = new DiscountingSwapLegPricer(mockPeriod, eventPricer);

    CashFlows computed = pricer.cashFlows(expSwapLeg, mockProv);
    CashFlow flow1 = CashFlow.ofForecastValue(period1.getPaymentDate(), GBP, fv1, df1);
    CashFlow flow2 = CashFlow.ofForecastValue(period2.getPaymentDate(), GBP, fv2, df2);
    CashFlow flow3 = CashFlow.ofForecastValue(event.getPaymentDate(), GBP, event.getPaymentAmount().getAmount(), df);
    CashFlows expected = CashFlows.of(ImmutableList.of(flow1, flow2, flow3));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivity(expSwapLeg, RATES_GBP).build();
    MultiCurrencyAmount expected = RATES_GBP.currencyExposure(point).plus(PRICER_LEG.presentValue(expSwapLeg, RATES_GBP));
    MultiCurrencyAmount computed = PRICER_LEG.currencyExposure(expSwapLeg, RATES_GBP);
    assertEquals(computed, expected);
  }

  public void test_currencyExposure_fx() {
    ResolvedSwapLeg expSwapLeg = FIXED_FX_RESET_SWAP_LEG_PAY_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivity(expSwapLeg, RATES_GBP_USD).build();
    MultiCurrencyAmount expected = RATES_GBP_USD.currencyExposure(point.convertedTo(USD, RATES_GBP_USD))
        .plus(PRICER_LEG.presentValue(expSwapLeg, RATES_GBP_USD));
    MultiCurrencyAmount computed = PRICER_LEG.currencyExposure(expSwapLeg, RATES_GBP_USD);
    assertEquals(computed.getAmount(USD).getAmount(), expected.getAmount(USD).getAmount(), EPS * NOTIONAL);
    assertFalse(computed.contains(GBP)); // 0 GBP
  }

  //-------------------------------------------------------------------------
  public void test_currentCash_zero() {
    ResolvedSwapLeg expSwapLeg = IBOR_SWAP_LEG_REC_GBP;
    CurrencyAmount computed = PRICER_LEG.currentCash(expSwapLeg, RATES_GBP);
    assertEquals(computed, CurrencyAmount.zero(expSwapLeg.getCurrency()));
  }

  public void test_currentCash_payEvent() {
    ResolvedSwapLeg expSwapLeg = FIXED_SWAP_LEG_PAY_USD;
    LocalDate paymentDate = expSwapLeg.getPaymentEvents().get(0).getPaymentDate();
    RatesProvider prov = new MockRatesProvider(paymentDate);
    SwapPaymentEventPricer<SwapPaymentEvent> mockEvent = mock(SwapPaymentEventPricer.class);
    double expected = 1234d;
    when(mockEvent.currentCash(expSwapLeg.getPaymentEvents().get(0), prov)).thenReturn(expected);
    DiscountingSwapLegPricer pricer = new DiscountingSwapLegPricer(SwapPaymentPeriodPricer.standard(), mockEvent);
    CurrencyAmount computed = pricer.currentCash(expSwapLeg, prov);
    assertEquals(computed, CurrencyAmount.of(expSwapLeg.getCurrency(), expected));
  }

  public void test_currentCash_payPeriod() {
    ResolvedSwapLeg expSwapLeg = FIXED_SWAP_LEG_PAY_USD;
    LocalDate paymentDate = expSwapLeg.getPaymentPeriods().get(0).getPaymentDate();
    RatesProvider prov = new MockRatesProvider(paymentDate);
    SwapPaymentPeriodPricer<SwapPaymentPeriod> mockPeriod = mock(SwapPaymentPeriodPricer.class);
    double expected = 1234d;
    when(mockPeriod.currentCash(expSwapLeg.getPaymentPeriods().get(0), prov)).thenReturn(expected);
    DiscountingSwapLegPricer pricer = new DiscountingSwapLegPricer(mockPeriod, SwapPaymentEventPricer.standard());
    CurrencyAmount computed = pricer.currentCash(expSwapLeg, prov);
    assertEquals(computed, CurrencyAmount.of(expSwapLeg.getCurrency(), expected));
  }  

  public void test_currentCash_convention() { // Check that standard conventions return a compounded ZC fixed leg
    FixedInflationSwapConvention US_CPI = FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI;
    double rate = 0.10;
    int nbYears = 5;
    LocalDate endDate = VAL_DATE_INFLATION.plusYears(nbYears);
    SwapTrade swap = US_CPI.toTrade(VAL_DATE_INFLATION, VAL_DATE_INFLATION, endDate, BuySell.BUY, NOTIONAL, rate);
    ResolvedSwap resolved = swap.getProduct().resolve(REF_DATA);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    RatesProvider providerEndDate = new MockRatesProvider(endDate);
    CurrencyAmount c = pricer.currentCash(resolved.getLegs(SwapLegType.FIXED).get(0), providerEndDate);
    assertEquals(c.getAmount(), -(Math.pow(1 + rate, nbYears) - 1.0) * NOTIONAL, NOTIONAL * EPS);
  }

  private static final int NB_PERIODS = 10;
  private static final int NB_PERIODS_PER_YEAR = 2;
  private static final double[] RATES = {0.01, 0.10, -0.01, 0.0, 1.0E-6 };
  private static final double TOLERANCE_ANNUITY = 1.0E-10;
  private static final double TOLERANCE_ANNUITY_1 = 1.0E-6;
  private static final double TOLERANCE_ANNUITY_2 = 1.0E-4;
  private static final double TOLERANCE_ANNUITY_3 = 1.0E-2;

  public void annuity_cash() {
    for (int looprate = 0; looprate < RATES.length; looprate++) {
      double annuityExpected = 0.0d;
      for (int loopperiod = 0; loopperiod < NB_PERIODS; loopperiod++) {
        annuityExpected += 1.0d / NB_PERIODS_PER_YEAR / Math.pow(1.0d + RATES[looprate] / NB_PERIODS_PER_YEAR, loopperiod + 1);
      }
      double annuityComputed = PRICER_LEG.annuityCash(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      assertEquals(annuityComputed, annuityExpected, TOLERANCE_ANNUITY, "Rate: " + looprate);
    }
  }

  public void annuity_cash_1() {
    double shift = 1.0E-7;
    for (int looprate = 0; looprate < RATES.length; looprate++) {
      double annuityExpected = PRICER_LEG.annuityCash(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      ValueDerivatives annuityComputed = PRICER_LEG.annuityCash1(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      assertEquals(annuityComputed.getValue(), annuityExpected, TOLERANCE_ANNUITY);
      double annuityP = PRICER_LEG.annuityCash(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] + shift);
      double annuityM = PRICER_LEG.annuityCash(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] - shift);
      double derivative1Expected = (annuityP - annuityM) / (2.0d * shift);
      assertEquals(annuityComputed.getDerivative(0), derivative1Expected, TOLERANCE_ANNUITY_1, "Rate: " + looprate);
    }
  }

  public void annuity_cash_2() {
    double shift = 1.0E-7;
    for (int looprate = 0; looprate < RATES.length; looprate++) {
      ValueDerivatives annuityExpected = PRICER_LEG.annuityCash1(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      ValueDerivatives annuityComputed = PRICER_LEG.annuityCash2(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      ValueDerivatives annuityP = PRICER_LEG.annuityCash1(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] + shift);
      ValueDerivatives annuityM = PRICER_LEG.annuityCash1(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] - shift);
      double derivative2Expected = (annuityP.getDerivative(0) - annuityM.getDerivative(0)) / (2.0d * shift);
      assertEquals(annuityComputed.getValue(), annuityExpected.getValue(), TOLERANCE_ANNUITY_1);
      assertEquals(annuityComputed.getDerivative(0), annuityExpected.getDerivative(0), TOLERANCE_ANNUITY_1);
      assertEquals(annuityComputed.getDerivative(1), derivative2Expected, TOLERANCE_ANNUITY_2);
    }
  }

  public void annuity_cash_3() {
    double shift = 1.0E-7;
    for (int looprate = 0; looprate < RATES.length; looprate++) {
      ValueDerivatives annuityExpected = PRICER_LEG.annuityCash2(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      ValueDerivatives annuityComputed = PRICER_LEG.annuityCash3(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate]);
      ValueDerivatives annuityP = PRICER_LEG.annuityCash2(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] + shift);
      ValueDerivatives annuityM = PRICER_LEG.annuityCash2(NB_PERIODS_PER_YEAR, NB_PERIODS, RATES[looprate] - shift);
      double derivative3Expected = (annuityP.getDerivative(1) - annuityM.getDerivative(1)) / (2.0d * shift);
      assertEquals(annuityComputed.getValue(), annuityExpected.getValue(), TOLERANCE_ANNUITY_1);
      assertEquals(annuityComputed.getDerivative(0), annuityExpected.getDerivative(0), TOLERANCE_ANNUITY_1);
      assertEquals(annuityComputed.getDerivative(1), annuityExpected.getDerivative(1), TOLERANCE_ANNUITY_2);
      assertEquals(annuityComputed.getDerivative(2), derivative3Expected, TOLERANCE_ANNUITY_3, "rate: " + looprate);
    }
  }

}
