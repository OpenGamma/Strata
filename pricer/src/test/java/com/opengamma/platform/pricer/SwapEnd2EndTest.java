package com.opengamma.platform.pricer;
/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConventions.PRECEDING;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.ser.JodaBeanSer;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.MarketQuoteSensitivityBlockCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DayCounts;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.date.HolidayCalendars;
import com.opengamma.basics.index.RateIndices;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.PeriodicSchedule;
import com.opengamma.basics.value.ValueSchedule;
import com.opengamma.platform.finance.trade.swap.FixedRateCalculation;
import com.opengamma.platform.finance.trade.swap.FixedRateSwapLeg;
import com.opengamma.platform.finance.trade.swap.FixingRelativeTo;
import com.opengamma.platform.finance.trade.swap.FloatingRateCalculation;
import com.opengamma.platform.finance.trade.swap.FloatingRateSwapLeg;
import com.opengamma.platform.finance.trade.swap.NotionalAmount;
import com.opengamma.platform.finance.trade.swap.PaymentSchedule;
import com.opengamma.platform.finance.trade.swap.SwapTrade;
import com.opengamma.platform.pricer.impl.StandardSwapPricerFn;
import com.opengamma.util.tuple.Pair;

/**
 * Test end to end.
 */
@Test
public class SwapEnd2EndTest {

  private static final IborIndex[] INDEX_IBOR_LIST = StandardDataSetsMulticurveUSD.indexIborArrayUSDOisL1L3L6();
  private static final IborIndex USDLIBOR1M = INDEX_IBOR_LIST[0];
  private static final IborIndex USDLIBOR3M = INDEX_IBOR_LIST[1];
  private static final IborIndex USDLIBOR6M = INDEX_IBOR_LIST[2];
  private static final com.opengamma.util.money.Currency USD = USDLIBOR3M.getCurrency();
  /** Calculators */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();
  private static final ParRateDiscountingCalculator PRDC = ParRateDiscountingCalculator.getInstance();
  private static final ParSpreadMarketQuoteDiscountingCalculator PSMQDC = 
      ParSpreadMarketQuoteDiscountingCalculator.getInstance();
  private static final PresentValueCurveSensitivityDiscountingCalculator PVCSDC = 
      PresentValueCurveSensitivityDiscountingCalculator.getInstance();
  private static final ParameterSensitivityParameterCalculator<MulticurveProviderInterface> PSC = 
      new ParameterSensitivityParameterCalculator<>(PVCSDC);
  private static final MarketQuoteSensitivityBlockCalculator<MulticurveProviderInterface> MQSBC = 
      new MarketQuoteSensitivityBlockCalculator<>(PSC);
  /** Curve providers */
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_OIS_PAIR = 
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL1L3L6();
  private static final MulticurveProviderDiscount MULTICURVE_OIS = MULTICURVE_OIS_PAIR.getFirst();
  private static final CurveBuildingBlockBundle BLOCK_OIS = MULTICURVE_OIS_PAIR.getSecond();
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_FF_PAIR = 
      StandardDataSetsMulticurveUSD.getCurvesUSDOisFFL1L3L6();
  private static final MulticurveProviderDiscount MULTICURVE_FFS = MULTICURVE_FF_PAIR.getFirst();
  private static final CurveBuildingBlockBundle BLOCK_FFS = MULTICURVE_FF_PAIR.getSecond();
  
  private static final double TOLERANCE_PV = 1.0E-3;
  private static final double TOLERANCE_PV_DELTA = 1.0E-4;
  private static final double TOLERANCE_RATE = 1.0E-8;
  private static final double BP1 = 1.0E-4;

  //-----------------------------------------------------------------------
  public void test_VanillaFixedVsLibor3mSwap() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY);
    BusinessDayAdjustment bdaPreceding = BusinessDayAdjustment.of(PRECEDING, HolidayCalendars.USNY);
    
    FixedRateSwapLeg payLeg = FixedRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FixedRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, -100_000_000)))
            .dayCount(DayCounts.THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    
    FloatingRateSwapLeg receiveLeg = FloatingRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FloatingRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, 100_000_000)))
            .dayCount(DayCounts.ACT_360)
            .index(RateIndices.USD_LIBOR_3M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.USNY, bdaPreceding))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .tradeDate(LocalDate.of(2014, 9, 10))
        .leg1(payLeg)
        .leg2(receiveLeg)
        .build();
    
    SwapPricerFn pricer = new StandardSwapPricerFn();
//    CurrencyAmount pv1 = pricer.presentValue(MULTICURVE_FFS, LocalDate.of(2014, 1, 22), trade);
//    assertEquals(pv1.getAmount(), 6065111.8810, TOLERANCE_PV);
    
    CurrencyAmount pv2 = pricer.presentValue(MULTICURVE_OIS, LocalDate.of(2014, 1, 22), trade);
    assertEquals(pv2.getAmount(), 7170391.798257509, TOLERANCE_PV);
  }

  public void test_VanillaFixedVsLibor3mSwapWithFixing() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY);
    BusinessDayAdjustment bdaPreceding = BusinessDayAdjustment.of(PRECEDING, HolidayCalendars.USNY);
    
    FixedRateSwapLeg payLeg = FixedRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FixedRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, -100_000_000)))
            .dayCount(DayCounts.THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    
    FloatingRateSwapLeg receiveLeg = FloatingRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FloatingRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, 100_000_000)))
            .dayCount(DayCounts.ACT_360)
            .index(RateIndices.USD_LIBOR_3M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.USNY, bdaPreceding))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .tradeDate(LocalDate.of(2014, 9, 10))
        .leg1(payLeg)
        .leg2(receiveLeg)
        .build();
    
    SwapPricerFn pricer = new StandardSwapPricerFn();
//    CurrencyAmount pv1 = pricer.presentValue(MULTICURVE_FFS, LocalDate.of(2014, 1, 22), trade);
//    assertEquals(pv1.getAmount(), 6065111.8810, TOLERANCE_PV);
    
    CurrencyAmount pv2 = pricer.presentValue(MULTICURVE_OIS, LocalDate.of(2014, 1, 22), trade);
    assertEquals(pv2.getAmount(), 3588376.471608199, TOLERANCE_PV);
  }

  public void test_BasisLibor3mVsLibor6mSwapWithSpread() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CalendarUSD.NYC);
    BusinessDayAdjustment bdaPreceding = BusinessDayAdjustment.of(PRECEDING, CalendarUSD.NYC);
    
    FloatingRateSwapLeg payLeg = FloatingRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FloatingRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, 100_000_000)))
            .dayCount(DayCounts.ACT_360)
            .index(RateIndices.USD_LIBOR_3M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, bdaPreceding))
            .spread(ValueSchedule.of(0.0010))
            .build())
        .build();
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(payLeg.toExpanded()));
    
    FloatingRateSwapLeg receiveLeg = FloatingRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 29))
            .endDate(LocalDate.of(2024, 8, 29))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(bda)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FloatingRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, -100_000_000)))
            .dayCount(DayCounts.ACT_360)
            .index(RateIndices.USD_LIBOR_6M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, bdaPreceding))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .tradeDate(LocalDate.of(2014, 9, 10))
        .leg1(payLeg)
        .leg2(receiveLeg)
        .build();
    
    SwapPricerFn pricer = new StandardSwapPricerFn();
//    CurrencyAmount pv1 = pricer.presentValue(MULTICURVE_FFS, LocalDate.of(2014, 1, 22), trade);
//    assertEquals(pv1.getAmount(), 6065111.8810, TOLERANCE_PV);
    
    CurrencyAmount pv2 = pricer.presentValue(MULTICURVE_OIS, LocalDate.of(2014, 1, 22), trade);
    assertEquals(pv2.getAmount(), -13844.3872, TOLERANCE_PV);
  }

}
