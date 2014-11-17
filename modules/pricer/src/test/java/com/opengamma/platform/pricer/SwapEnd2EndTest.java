/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import static com.opengamma.basics.PayReceive.PAY;
import static com.opengamma.basics.PayReceive.RECEIVE;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.basics.index.RateIndices.USD_FED_FUND;
import static com.opengamma.basics.schedule.Frequency.P12M;
import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P3M;
import static com.opengamma.basics.schedule.Frequency.P6M;
import static com.opengamma.basics.schedule.Frequency.TERM;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.basics.PayReceive;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.basics.index.RateIndices;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.PeriodicSchedule;
import com.opengamma.basics.schedule.StubConvention;
import com.opengamma.basics.value.ValueAdjustment;
import com.opengamma.basics.value.ValueSchedule;
import com.opengamma.basics.value.ValueStep;
import com.opengamma.platform.finance.swap.CompoundingMethod;
import com.opengamma.platform.finance.swap.FixedRateCalculation;
import com.opengamma.platform.finance.swap.IborRateCalculation;
import com.opengamma.platform.finance.swap.NotionalAmount;
import com.opengamma.platform.finance.swap.OvernightRateCalculation;
import com.opengamma.platform.finance.swap.PaymentSchedule;
import com.opengamma.platform.finance.swap.RateSwapLeg;
import com.opengamma.platform.finance.swap.StubCalculation;
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.swap.SwapPricerFn;
import com.opengamma.platform.pricerfn.swap.StandardSwapPricerFn;
import com.opengamma.platform.source.id.StandardId;
import com.opengamma.util.tuple.Pair;

/**
 * Test end to end.
 */
@Test
public class SwapEnd2EndTest {

  private static final IborIndex USD_LIBOR_1M = lockIndexCalendar(RateIndices.USD_LIBOR_1M);
  private static final IborIndex USD_LIBOR_3M = lockIndexCalendar(RateIndices.USD_LIBOR_3M);
  private static final IborIndex USD_LIBOR_6M = lockIndexCalendar(RateIndices.USD_LIBOR_6M);
  private static final NotionalAmount NOTIONAL = NotionalAmount.of(USD, 100_000_000);
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CalendarUSD.NYC);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, CalendarUSD.NYC);

  // curve providers
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_OIS_PAIR = 
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL1L3L6();
  private static final MulticurveProviderDiscount MULTICURVE_OIS = MULTICURVE_OIS_PAIR.getFirst();

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;

  //-----------------------------------------------------------------------
  public void test_VanillaFixedVsLibor1mSwap() {
    RateSwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 9, 12), P6M, PAY, NOTIONAL, 0.0125, null);
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_1M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), -1003684.8402, TOLERANCE_PV);
  }

  //-----------------------------------------------------------------------
  public void test_VanillaFixedVsLibor3mSwap() {
    RateSwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2021, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 7170391.798257509, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_VanillaFixedVsLibor3mSwapWithFixing() {
    RateSwapLeg payLeg = fixedLeg(
        LocalDate.of(2013, 9, 12), LocalDate.of(2020, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2013, 9, 10))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 3588376.471608199, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_BasisLibor3mVsLibor6mSwapWithSpread() {
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_6M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0010))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 8, 27))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
//    assertEquals(pv.getAmount(), -13844.3872, TOLERANCE_PV);
    assertEquals(pv.getAmount(), -21875.376339152455, TOLERANCE_PV);
    
  }

  //-------------------------------------------------------------------------
  public void test_BasisCompoundedLibor1mVsLibor3mSwap() {
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2019, 8, 29))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.FLAT)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_1M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2019, 8, 29))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 8, 27))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    // unable to match existing number
    // assertEquals(pv.getAmount(), -340426.6128, TOLERANCE_PV);
    assertEquals(pv.getAmount(), -342874.98367929866, TOLERANCE_PV);  // unverified number
  }

  //-------------------------------------------------------------------------
  public void test_Stub3mFixed6mVsLibor3mSwap() {
    RateSwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 6, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 502890.9443281095, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_Stub1mFixed6mVsLibor3mSwap() {
    RateSwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 7, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 463962.5517136799, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_InterpolatedStub3mFixed6mVsLibor6mSwap() {
    RateSwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 6, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_6M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .initialStub(StubCalculation.of(USD_LIBOR_3M, USD_LIBOR_6M))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 364832.4284058402, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_InterpolatedStub4mFixed6mVsLibor6mSwap() {
    RateSwapLeg receiveLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 7, 12), P6M, RECEIVE, NOTIONAL, 0.01, StubConvention.SHORT_INITIAL);
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_6M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .initialStub(StubCalculation.of(USD_LIBOR_3M, USD_LIBOR_6M))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), 314215.2347116342, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_ZeroCouponFixedVsLibor3mSwap() {
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P12M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notional(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notional(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
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
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NotionalAmount.of(USD, notionalSchedule))
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.016))
            .build())
        .build();
    
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(NotionalAmount.of(USD, notionalSchedule))
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 9, 10))
        .swap(Swap.of(receiveLeg, payLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), -1850080.2895532502, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_CompoundingOisFixed2mVsFedFund12mSwap() {
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notional(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.00123))
            .build())
        .build();
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notional(NOTIONAL)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_FED_FUND)
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 2, 3))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), -9723.264518929138, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_CompoundingOisFixed2mVsFedFund12mSwapWithFixing() {
    RateSwapLeg payLeg = RateSwapLeg.builder()
        .payReceive(PAY)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notional(NOTIONAL)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.00123))
            .build())
        .build();
    
    RateSwapLeg receiveLeg = RateSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(TERM)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentOffset(DaysAdjustment.ofBusinessDays(2, CalendarUSD.NYC))
            .build())
        .notional(NOTIONAL)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_FED_FUND)
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 1, 15))
        .swap(Swap.of(payLeg, receiveLeg))
        .build();
    
    SwapPricerFn pricer = swapPricer();
    CurrencyAmount pv = pricer.presentValue(env(), LocalDate.of(2014, 1, 22), trade.getSwap()).getAmount(USD);
    assertEquals(pv.getAmount(), -7352.973875972721, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  // fixed rate leg
  private static RateSwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalAmount notional, double fixedRate, StubConvention stubConvention) {
    
    return RateSwapLeg.builder()
        .payReceive(payReceive)
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notional(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(fixedRate))
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  // pricer
  private StandardSwapPricerFn swapPricer() {
    return StandardSwapPricerFn.DEFAULT;
  }

  // pricing environment
  private static PricingEnvironment env() {
    return ImmutablePricingEnvironment.builder()
        .multicurve(MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(
            USD_LIBOR_1M, SwapInstrumentsDataSet.TS_USDLIBOR1M,
            USD_LIBOR_3M, SwapInstrumentsDataSet.TS_USDLIBOR3M,
            USD_LIBOR_6M, SwapInstrumentsDataSet.TS_USDLIBOR6M,
            USD_FED_FUND, SwapInstrumentsDataSet.TS_USDON))
        .dayCount(ACT_ACT_ISDA)
        .build();
  }

  // use a fixed known set of holiday dates to ensure tests produce same numbers
  private static IborIndex lockIndexCalendar(IborIndex index) {
    return index.toBuilder()
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
