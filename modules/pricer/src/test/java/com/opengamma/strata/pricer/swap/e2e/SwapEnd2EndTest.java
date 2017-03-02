/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap.e2e;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.datasets.StandardDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test end to end.
 */
@Test
public class SwapEnd2EndTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ImmutableReferenceData.of(CalendarUSD.NYC, CalendarUSD.NYC_CALENDAR));
  private static final LocalDate VAL_DATE = StandardDataSets.VAL_DATE_2014_01_22;
  static final IborIndex USD_LIBOR_1M = lockIndexCalendar(IborIndices.USD_LIBOR_1M);
  static final IborIndex USD_LIBOR_3M = lockIndexCalendar(IborIndices.USD_LIBOR_3M);
  static final IborIndex USD_LIBOR_6M = lockIndexCalendar(IborIndices.USD_LIBOR_6M);
  static final NotionalSchedule NOTIONAL = NotionalSchedule.of(USD, 100_000_000);
  static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CalendarUSD.NYC);
  static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, CalendarUSD.NYC);
  private static final LocalDateDoubleTimeSeries TS_USDLIBOR1M =
      LocalDateDoubleTimeSeries.builder()
          .put(LocalDate.of(2013, 12, 10), 0.00123)
          .put(LocalDate.of(2013, 12, 12), 0.00123)
          .build();
  private static final LocalDateDoubleTimeSeries TS_USDLIBOR3M =
      LocalDateDoubleTimeSeries.builder()
          .put(LocalDate.of(2013, 12, 10), 0.0024185)
          .put(LocalDate.of(2013, 12, 12), 0.0024285)
          .build();
  private static final LocalDateDoubleTimeSeries TS_USDLIBOR6M =
      LocalDateDoubleTimeSeries.builder()
          .put(LocalDate.of(2013, 12, 10), 0.0030)
          .put(LocalDate.of(2013, 12, 12), 0.0035)
          .build();
  private static final LocalDateDoubleTimeSeries TS_USDON =
      LocalDateDoubleTimeSeries.builder()
          .put(LocalDate.of(2014, 1, 17), 0.0007)
          .put(LocalDate.of(2014, 1, 21), 0.0007)
          .put(VAL_DATE, 0.0007)
          .build();

  private static final DiscountingSwapProductPricer PRICER_PRODUCT =
      DiscountingSwapProductPricer.DEFAULT;

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;
  private static final double TOLERANCE_RATE = 1.0E-10;

  //-----------------------------------------------------------------------
  public void test_VanillaFixedVsLibor1mSwap() {
    SwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 9, 12), P6M, PAY, NOTIONAL, 0.0125, null);

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_1M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -1003684.8402, TOLERANCE_PV);
  }

  //-----------------------------------------------------------------------
  public void test_VanillaFixedVsLibor3mSwap() {
    SwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2021, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);
    SwapLeg receiveLeg = iborLeg(LocalDate.of(2014, 9, 12), LocalDate.of(2021, 9, 12),
        USD_LIBOR_3M, RECEIVE, NOTIONAL, null);
    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    // test pv
    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 7170391.798257509, TOLERANCE_PV);
    // test par rate
    double parRate = PRICER_PRODUCT.parRate(trade.getProduct(), provider());
    assertEquals(parRate, 0.02589471566819517, TOLERANCE_RATE);
    // test par rate vs pv
    ResolvedSwap swapPV0 =
        Swap.of(fixedLeg(LocalDate.of(2014, 9, 12), LocalDate.of(2021, 9, 12), P6M, PAY, NOTIONAL, parRate, null), receiveLeg)
            .resolve(REF_DATA);
    CurrencyAmount pv0 = PRICER_PRODUCT.presentValue(swapPV0, provider()).getAmount(USD);
    assertEquals(pv0.getAmount(), 0, TOLERANCE_PV); // PV at par rate should be 0
  }

  //-------------------------------------------------------------------------
  public void test_VanillaFixedVsLibor3mSwapWithFixing() {
    SwapLeg payLeg = fixedLeg(
        LocalDate.of(2013, 9, 12), LocalDate.of(2020, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2013, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 3588376.471608199, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_BasisLibor3mVsLibor6mSwapWithSpread() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_6M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0010))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 8, 27)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -21875.376339152455, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_BasisCompoundedLibor1mVsLibor3mSwap() {
    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2019, 8, 29))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.FLAT)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_1M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2019, 8, 29))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 8, 27)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -342874.98367929866, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_Stub3mFixed6mVsLibor3mSwap() {
    SwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 6, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 502890.9443281095, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_Stub1mFixed6mVsLibor3mSwap() {
    SwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 7, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 463962.5517136799, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_InterpolatedStub3mFixed6mVsLibor6mSwap() {
    SwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 6, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_6M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .initialStub(IborRateStubCalculation.ofIborInterpolatedRate(USD_LIBOR_3M, USD_LIBOR_6M))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 364832.4284058402, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_InterpolatedStub4mFixed6mVsLibor6mSwap() {
    SwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 7, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_6M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .initialStub(IborRateStubCalculation.ofIborInterpolatedRate(USD_LIBOR_3M, USD_LIBOR_6M))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 314215.2347116342, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_ZeroCouponFixedVsLibor3mSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P12M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), 7850279.042216873, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_AmortizingFixedVsLibor3mSwap() {
    ValueAdjustment stepReduction = ValueAdjustment.ofDeltaAmount(-3_000_000);
    List<ValueStep> steps = new ArrayList<>();
    for (int i = 1; i < 28; i++) {
      steps.add(ValueStep.of(i, stepReduction));
    }
    ValueSchedule notionalSchedule = ValueSchedule.of(100_000_000, steps);
    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(USD, notionalSchedule))
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.016))
            .build())
        .build();

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(USD, notionalSchedule))
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(receiveLeg, payLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -1850080.2895532502, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_CompoundingOisFixed2mVsFedFund12mSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.00123))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_FED_FUND)
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 2, 3)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -9723.264518929138, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_CompoundingOisFixed2mVsFedFund12mSwapWithFixing() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.00123))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_FED_FUND)
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 1, 15)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -7352.973875972721, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_OnAASpreadVsLibor3MSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_FED_FUND)
            .accrualMethod(OvernightAccrualMethod.AVERAGED)
            .rateCutOffDays(0) // Should be 2, put to 0 for comparison
            .spread(ValueSchedule.of(0.0025))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 1, 15)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(trade, provider()).getAmount(USD);
    assertEquals(pv.getAmount(), -160663.8362, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  // fixed rate leg
  private static SwapLeg fixedLeg(
      LocalDate start,
      LocalDate end,
      Frequency frequency,
      PayReceive payReceive,
      NotionalSchedule notional,
      double fixedRate,
      StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(fixedRate))
            .build())
        .build();
  }

  // ibor rate leg
  private static SwapLeg iborLeg(
      LocalDate start,
      LocalDate end,
      IborIndex index,
      PayReceive payReceive,
      NotionalSchedule notional,
      StubConvention stubConvention) {

    Frequency freq = Frequency.of(index.getTenor().getPeriod());
    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(freq)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(freq)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(index)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, index.getFixingCalendar(), BDA_P))
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  // pricer
  static DiscountingSwapTradePricer swapPricer() {
    return DiscountingSwapTradePricer.DEFAULT;
  }

  // rates provider
  static RatesProvider provider() {
    // StandardDataSets.providerUsdDscOnL1L3L6() with locked holidays and time-series
    return ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(StandardDataSets.FX_MATRIX)
        .discountCurve(USD, StandardDataSets.GROUP1_USD_DSC)
        .overnightIndexCurve(USD_FED_FUND, StandardDataSets.GROUP1_USD_ON, TS_USDON)
        .iborIndexCurve(USD_LIBOR_1M, StandardDataSets.GROUP1_USD_L1M, TS_USDLIBOR1M)
        .iborIndexCurve(USD_LIBOR_3M, StandardDataSets.GROUP1_USD_L3M, TS_USDLIBOR3M)
        .iborIndexCurve(USD_LIBOR_6M, StandardDataSets.GROUP1_USD_L6M, TS_USDLIBOR6M)
        .build();
  }

  // use a fixed known set of holiday dates to ensure tests produce same numbers
  private static IborIndex lockIndexCalendar(IborIndex index) {
    return ((ImmutableIborIndex) index).toBuilder()
        .fixingCalendar(CalendarUSD.NYC)
        .effectiveDateOffset(index.getEffectiveDateOffset().toBuilder()
            .calendar(CalendarUSD.NYC)
            .adjustment(index.getEffectiveDateOffset().getAdjustment().toBuilder()
                .calendar(CalendarUSD.NYC)
                .build())
            .build())
        .maturityDateOffset(index.getMaturityDateOffset().toBuilder()
            .adjustment(index.getMaturityDateOffset().getAdjustment().toBuilder()
                .calendar(CalendarUSD.NYC)
                .build())
            .build())
        .build();
  }

}
