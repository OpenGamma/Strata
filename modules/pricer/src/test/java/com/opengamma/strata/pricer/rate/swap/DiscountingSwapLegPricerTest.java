/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.swap.CompoundingMethod.STRAIGHT;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC_GBP_MULTI;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.InflationRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.ForwardPriceIndexValues;
import com.opengamma.strata.market.value.PriceIndexValues;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.impl.rate.ForwardInflationInterpolatedRateObservationFn;
import com.opengamma.strata.pricer.impl.rate.ForwardInflationMonthlyRateObservationFn;
import com.opengamma.strata.pricer.impl.rate.swap.DispatchingPaymentEventPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

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

  //-------------------------------------------------------------------------
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
    assertThrowsIllegalArg(() -> test.pvbp(FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbp_Compounding() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbp(FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD, MOCK_PROV));
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
    CurveParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivities psFd =
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
    CurveParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivities psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP,
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValueEventsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  public void test_presentValueSensitivity_periods() {
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP;
    PointSensitivities point = PRICER_LEG.presentValueSensitivityPeriodsInternal(expSwapLeg, RATES_GBP).build();
    CurveParameterSensitivities psAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivities psFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_GBP,
        (p) -> CurrencyAmount.of(GBP, PRICER_LEG.presentValuePeriodsInternal(expSwapLeg, p)));
    assertTrue(psAd.equalWithTolerance(psFd, TOLERANCE_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_pvbpSensitivity() {
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD_2)
        .build();
    PointSensitivities point = PRICER_LEG.pvbpSensitivity(leg, RATES_USD).build();
    CurveParameterSensitivities pvbpsAd = RATES_USD.parameterSensitivity(point);
    CurveParameterSensitivities pvbpsFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(RATES_USD,
        (p) -> CurrencyAmount.of(USD, PRICER_LEG.pvbp(leg, p)));
    assertTrue(pvbpsAd.equalWithTolerance(pvbpsFd, TOLERANCE_DELTA));
  }

  public void test_pvbpSensitivity_FxReset() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbpSensitivity(FIXED_FX_RESET_EXPANDED_SWAP_LEG_PAY_GBP, MOCK_PROV));
  }

  public void test_pvbpSensitivity_Compounding() {
    DiscountingSwapLegPricer test = DiscountingSwapLegPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.pvbpSensitivity(FIXED_CMP_EXPANDED_SWAP_LEG_PAY_USD, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
  private static final LocalDate DATE_14_06_09 = date(2014, 6, 9);
  private static final LocalDate DATE_19_06_09 = date(2019, 6, 9);
  private static final LocalDate DATE_14_03_31 = date(2014, 3, 31);
  private static final double START_INDEX = 218.0;
  private static final double NOTIONAL = 1000d;
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 7, 8);
  private static final YearMonth VAL_MONTH = YearMonth.from(VAL_DATE);

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final double CONSTANT_INDEX = 242.0;
  private static final PriceIndexValues GBPRI_CURVE_FLAT = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(VAL_DATE.minusMonths(3), START_INDEX),
      InterpolatedNodalCurve.of(
          "GB_RPI_CURVE",
          new double[] {1, 200},
          new double[] {CONSTANT_INDEX, CONSTANT_INDEX},
          INTERPOLATOR));

  private static final CurveInterpolator INTERP_SPLINE = Interpolator1DFactory.NATURAL_CUBIC_SPLINE_INSTANCE;
  private static final PriceIndexValues GBPRI_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(VAL_DATE.minusMonths(3), 227.2),
      InterpolatedNodalCurve.of(
          "GB_RPI_CURVE",
          new double[] {6, 12, 24, 60, 120},
          new double[] {227.2, 252.6, 289.5, 323.1, 351.1},
          INTERP_SPLINE));

  private static final double EPS = 1.0e-14;

  public void test_inflation_monthly() {
    // setup
    SwapLeg swapLeg = createInflationSwapLeg(false, PAY);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, GBPRI_CURVE_FLAT);
    Map<Currency, Curve> dscCurve = RATES_GBP.getDiscountCurves();
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(DATE_14_03_31, START_INDEX);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(GB_RPI, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(dscCurve)
        .dayCount(ACT_ACT_ISDA)
        .build();
    // test futureValue and presentValue
    CurrencyAmount fvComputed = pricer.futureValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.expand().getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    double fvExpected = (CONSTANT_INDEX / START_INDEX - 1.0) * (-NOTIONAL);
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = dscFactor * fvExpected;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test futureValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.futureValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;
    RatePaymentPeriod paymentPeriod = (RatePaymentPeriod) swapLeg.expand().getPaymentPeriods().get(0);
    InflationMonthlyRateObservation obs =
        (InflationMonthlyRateObservation) paymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    PointSensitivityBuilder pvSensiExpected = obsFn.rateSensitivity(obs, DATE_14_06_09, DATE_19_06_09, prov);
    pvSensiExpected = pvSensiExpected.multipliedBy(-NOTIONAL);
    assertTrue(fvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
    pvSensiExpected = pvSensiExpected.multipliedBy(dscFactor);
    PointSensitivityBuilder dscSensiExpected = prov.discountFactors(GBP).pointSensitivity(paymentDate);
    dscSensiExpected = dscSensiExpected.multipliedBy(fvExpected);
    pvSensiExpected = pvSensiExpected.combinedWith(dscSensiExpected);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
  }

  public void test_inflation_interpolated() {
    // setup
    SwapLeg swapLeg = createInflationSwapLeg(true, RECEIVE);
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    Map<Currency, Curve> dscCurve = RATES_GBP.getDiscountCurves();
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(DATE_14_03_31, START_INDEX);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(GB_RPI, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(dscCurve)
        .dayCount(ACT_ACT_ISDA)
        .build();
    // test futureValue and presentValue
    CurrencyAmount fvComputed = pricer.futureValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.expand().getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    ForwardInflationInterpolatedRateObservationFn obsFn = ForwardInflationInterpolatedRateObservationFn.DEFAULT;
    RatePaymentPeriod paymentPeriod = (RatePaymentPeriod) swapLeg.expand().getPaymentPeriods().get(0);
    InflationInterpolatedRateObservation obs =
        (InflationInterpolatedRateObservation) paymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    double indexRate = obsFn.rate(obs, DATE_14_06_09, DATE_19_06_09, prov);
    double fvExpected = indexRate * (NOTIONAL);
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = dscFactor * fvExpected;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test futureValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.futureValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiExpected = obsFn.rateSensitivity(obs, DATE_14_06_09, DATE_19_06_09, prov);
    pvSensiExpected = pvSensiExpected.multipliedBy(NOTIONAL);
    assertTrue(fvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
    pvSensiExpected = pvSensiExpected.multipliedBy(dscFactor);
    PointSensitivityBuilder dscSensiExpected = prov.discountFactors(GBP).pointSensitivity(paymentDate);
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
        .interpolated(interpolated)
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
    SwapLeg swapLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
    Map<Currency, Curve> dscCurve = RATES_GBP.getDiscountCurves();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .discountCurves(dscCurve)
        .dayCount(ACT_ACT_ISDA)
        .build();
    // test futureValue and presentValue
    CurrencyAmount fvComputed = pricer.futureValue(swapLeg, prov);
    CurrencyAmount pvComputed = pricer.presentValue(swapLeg, prov);
    LocalDate paymentDate = swapLeg.expand().getPaymentPeriods().get(0).getPaymentDate();
    double dscFactor = prov.discountFactor(GBP, paymentDate);
    double fvExpected = (Math.pow(1.0 + fixedRate, 5) - 1.0) * NOTIONAL;
    assertEquals(fvComputed.getCurrency(), GBP);
    assertEquals(fvComputed.getAmount(), fvExpected, NOTIONAL * EPS);
    double pvExpected = fvExpected * dscFactor;
    assertEquals(pvComputed.getCurrency(), GBP);
    assertEquals(pvComputed.getAmount(), pvExpected, NOTIONAL * EPS);
    // test futureValueSensitivity and presentValueSensitivity
    PointSensitivityBuilder fvSensiComputed = pricer.futureValueSensitivity(swapLeg, prov);
    PointSensitivityBuilder pvSensiComputed = pricer.presentValueSensitivity(swapLeg, prov);
    assertEquals(fvSensiComputed, PointSensitivityBuilder.none());
    PointSensitivityBuilder pvSensiExpected = prov.discountFactors(GBP).pointSensitivity(paymentDate);
    pvSensiExpected = pvSensiExpected.multipliedBy(fvExpected);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), EPS * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_cashFlows() {
    RatesProvider mockProv = mock(RatesProvider.class);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    DispatchingPaymentEventPricer eventPricer = DispatchingPaymentEventPricer.DEFAULT;
    ExpandedSwapLeg expSwapLeg = IBOR_EXPANDED_SWAP_LEG_REC_GBP_MULTI;
    PaymentPeriod period1 = IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
    PaymentPeriod period2 = IBOR_RATE_PAYMENT_PERIOD_REC_GBP_2;
    NotionalExchange event = NOTIONAL_EXCHANGE_REC_GBP;
    double fv1 = 520d;
    double fv2 = 450d;
    double df = 1.0d;
    double df1 = 0.98;
    double df2 = 0.93;
    when(mockPeriod.futureValue(period1, mockProv)).thenReturn(fv1);
    when(mockPeriod.futureValue(period2, mockProv)).thenReturn(fv2);
    when(mockProv.getValuationDate()).thenReturn(LocalDate.of(2014, 7, 1));
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), period1.getPaymentDate())).thenReturn(df1);
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), period2.getPaymentDate())).thenReturn(df2);
    when(mockProv.discountFactor(expSwapLeg.getCurrency(), event.getPaymentDate())).thenReturn(df);
    DiscountingSwapLegPricer pricer = new DiscountingSwapLegPricer(mockPeriod, eventPricer);

    CashFlows computed = pricer.cashFlows(expSwapLeg, mockProv);
    CashFlow flow1 = CashFlow.of(period1.getPaymentDate(), GBP, fv1, df1);
    CashFlow flow2 = CashFlow.of(period2.getPaymentDate(), GBP, fv2, df2);
    CashFlow flow3 = CashFlow.of(event.getPaymentDate(), GBP, event.getPaymentAmount().getAmount(), df);
    CashFlows expected = CashFlows.of(ImmutableList.of(flow1, flow2, flow3));
    assertEquals(computed, expected);
  }

}
