/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap.e2e;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.BDA_MF;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.BDA_P;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.NOTIONAL;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.USD_LIBOR_1M;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.USD_LIBOR_3M;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.USD_LIBOR_6M;
import static com.opengamma.strata.pricer.swap.e2e.SwapEnd2EndTest.swapPricer;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;

import java.time.LocalDate;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Vague performance test.
 */
public class SwapPricePerformance {

  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ImmutableReferenceData.of(CalendarUSD.NYC, CalendarUSD.NYC_CALENDAR));

  public static void main(String[] args) throws Exception {
    System.out.println("Go");
    for (int i = 0; i < 12; i++) {
      if (process() > 0) {
        System.out.println(i);
      }
    }
  }

  private static double process() {
    SwapPricePerformance test = new SwapPricePerformance();
    long start = System.nanoTime();
    double total = 0d;
    for (int i = 0; i < 10_000; i++) {
      total += test.test_VanillaFixedVsLibor1mSwap();
      total += test.test_VanillaFixedVsLibor3mSwap();
      total += test.test_VanillaFixedVsLibor3mSwapWithFixing();
      total += test.test_BasisLibor3mVsLibor6mSwapWithSpread();
      total += test.test_BasisCompoundedLibor1mVsLibor3mSwap();
    }
    System.out.println("Total: " + total);
    long end = System.nanoTime();
    System.out.println((end - start) / 1_000_000_000d + " s");
    return total;
  }

  //-----------------------------------------------------------------------
  private static final SwapLeg PAY1 = fixedLeg(
      LocalDate.of(2014, 9, 12), LocalDate.of(2016, 9, 12), P6M, PAY, NOTIONAL, 0.0125, null);

  private static final SwapLeg RECEIVE1 = RateCalculationSwapLeg.builder()
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

  private static final SwapTrade TRADE1 = SwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
      .product(Swap.of(PAY1, RECEIVE1))
      .build();
  private static final RatesProvider PROVIDER = SwapEnd2EndTest.provider();

  public double test_VanillaFixedVsLibor1mSwap() {
    DiscountingSwapTradePricer pricer = swapPricer();
    ResolvedSwapTrade resolved = TRADE1.resolve(REF_DATA);
    CurrencyAmount pv = pricer.presentValue(resolved, USD, PROVIDER);
    return pv.getAmount();
  }

  //-----------------------------------------------------------------------
  private static final SwapLeg PAY2 = fixedLeg(
      LocalDate.of(2014, 9, 12), LocalDate.of(2021, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);

  private static final SwapLeg RECEIVE2 = RateCalculationSwapLeg.builder()
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
      .notionalSchedule(NOTIONAL)
      .calculation(IborRateCalculation.builder()
          .index(USD_LIBOR_3M)
          .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
          .build())
      .build();

  private static final SwapTrade TRADE2 = SwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
      .product(Swap.of(PAY2, RECEIVE2))
      .build();

  public double test_VanillaFixedVsLibor3mSwap() {
    DiscountingSwapTradePricer pricer = swapPricer();
    ResolvedSwapTrade resolved = TRADE2.resolve(REF_DATA);
    CurrencyAmount pv = pricer.presentValue(resolved, USD, PROVIDER);
    return pv.getAmount();
  }

  //-------------------------------------------------------------------------
  private static final SwapLeg PAY3 = fixedLeg(
      LocalDate.of(2013, 9, 12), LocalDate.of(2020, 9, 12), P6M, PAY, NOTIONAL, 0.015, null);

  private static final SwapLeg RECEIVE3 = RateCalculationSwapLeg.builder()
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

  private static final SwapTrade TRADE3 = SwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(LocalDate.of(2013, 9, 10)).build())
      .product(Swap.of(PAY3, RECEIVE3))
      .build();

  public double test_VanillaFixedVsLibor3mSwapWithFixing() {
    DiscountingSwapTradePricer pricer = swapPricer();
    ResolvedSwapTrade resolved = TRADE3.resolve(REF_DATA);
    CurrencyAmount pv = pricer.presentValue(resolved, USD, PROVIDER);
    return pv.getAmount();
  }

  //-------------------------------------------------------------------------
  private static final SwapLeg PAY4 = RateCalculationSwapLeg.builder()
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

  private static final SwapLeg RECEIVE4 = RateCalculationSwapLeg.builder()
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

  private static final SwapTrade TRADE4 = SwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 8, 27)).build())
      .product(Swap.of(PAY4, RECEIVE4))
      .build();

  public double test_BasisLibor3mVsLibor6mSwapWithSpread() {
    DiscountingSwapTradePricer pricer = swapPricer();
    ResolvedSwapTrade resolved = TRADE4.resolve(REF_DATA);
    CurrencyAmount pv = pricer.presentValue(resolved, USD, PROVIDER);
    return pv.getAmount();
  }

  //-------------------------------------------------------------------------
  private static final SwapLeg RECEIVE5 = RateCalculationSwapLeg.builder()
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

  private static final SwapLeg PAY5 = RateCalculationSwapLeg.builder()
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

  private static final SwapTrade TRADE5 = SwapTrade.builder()
      .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 8, 27)).build())
      .product(Swap.of(RECEIVE5, PAY5))
      .build();

  public double test_BasisCompoundedLibor1mVsLibor3mSwap() {
    DiscountingSwapTradePricer pricer = swapPricer();
    ResolvedSwapTrade resolved = TRADE5.resolve(REF_DATA);
    CurrencyAmount pv = pricer.presentValue(resolved, USD, PROVIDER);
    return pv.getAmount();
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

}
