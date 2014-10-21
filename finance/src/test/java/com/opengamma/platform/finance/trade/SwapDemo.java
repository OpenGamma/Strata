/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade;

import static com.opengamma.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConventions.PRECEDING;

import java.time.LocalDate;

import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DayCounts;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.date.HolidayCalendars;
import com.opengamma.basics.index.RateIndices;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.PeriodicSchedule;
import com.opengamma.basics.schedule.RollConventions;
import com.opengamma.basics.schedule.StubConvention;
import com.opengamma.basics.value.ValueSchedule;
import com.opengamma.basics.value.ValueStep;
import com.opengamma.platform.finance.trade.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.trade.swap.FixedRateCalculation;
import com.opengamma.platform.finance.trade.swap.FixedRateSwapLeg;
import com.opengamma.platform.finance.trade.swap.FixingRelativeTo;
import com.opengamma.platform.finance.trade.swap.FloatingRateCalculation;
import com.opengamma.platform.finance.trade.swap.FloatingRateSwapLeg;
import com.opengamma.platform.finance.trade.swap.NotionalAmount;
import com.opengamma.platform.finance.trade.swap.PaymentSchedule;
import com.opengamma.platform.finance.trade.swap.SwapTrade;

/**
 * Demonstrate use of the API.
 */
public class SwapDemo {

  //-----------------------------------------------------------------------
  @Test(description = "Demo use of FixedRateSwapLeg")
  public void test_fixedSwapLeg() {
    BusinessDayAdjustment scheduleBda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.EUTA);
    BusinessDayAdjustment paymentBda = BusinessDayAdjustment.of(FOLLOWING, HolidayCalendars.EUTA);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 2, 12))
        .endDate(LocalDate.of(2016, 7, 31))
        .businessDayAdjustment(scheduleBda)
        .frequency(Frequency.P3M)
        .stubConvention(StubConvention.LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .build();
    FixedRateSwapLeg swapLeg = FixedRateSwapLeg.builder()
        .accrualPeriods(accrualSchedule)
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.ofCalendarDays(2, paymentBda))
            .build())
        .calculation(FixedRateCalculation.builder()
            .notional(NotionalAmount.builder()
                .currency(Currency.GBP)
                .amount(ValueSchedule.of(
                    1_000_000,
                    ImmutableList.of(ValueStep.ofAbsoluteAmount(LocalDate.parse("2014-07-31"), 2_000_000))))
                .build())
            .dayCount(DayCounts.ACT_ACT_ISDA)
            .rate(ValueSchedule.of(
                0.008,
                ImmutableList.of(ValueStep.ofAbsoluteAmount(LocalDate.parse("2014-07-31"), 0.007))))
            .build())
        .build();
    ExpandedSwapLeg expandedLeg = swapLeg.toExpanded();
    
    System.out.println("===== Fixed =====");
//    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(accrualSchedule));
//    System.out.println();
//    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(accrualSchedule.createSchedule()));
//    System.out.println();
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(swapLeg));
    System.out.println();
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(expandedLeg));
  }

  //-----------------------------------------------------------------------
  @Test(description = "Demo use of FixedRateSwapLeg")
  public void test_floatingSwapLeg() {
    BusinessDayAdjustment scheduleBda =
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.EUTA);
    BusinessDayAdjustment paymentBda =
        BusinessDayAdjustment.of(FOLLOWING, HolidayCalendars.EUTA);
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 2, 12))
        .endDate(LocalDate.of(2016, 7, 31))
        .businessDayAdjustment(scheduleBda)
        .frequency(Frequency.P3M)
        .stubConvention(StubConvention.LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .build();
    FloatingRateSwapLeg swapLeg = FloatingRateSwapLeg.builder()
        .accrualPeriods(accrualSchedule)
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.ofCalendarDays(2, paymentBda))
            .build())
        .calculation(FloatingRateCalculation.builder()
            .notional(NotionalAmount.builder()
                .currency(Currency.GBP)
                .amount(ValueSchedule.of(
                    1_000_000,
                    ImmutableList.of(ValueStep.ofAbsoluteAmount(LocalDate.parse("2014-07-31"), 2_000_000))))
                .build())
            .dayCount(DayCounts.ACT_ACT_ISDA)
            .index(RateIndices.EURIBOR_3M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.EUTA))
            .build())
        .build();
    ExpandedSwapLeg expandedLeg = swapLeg.toExpanded();
        
    System.out.println("===== Floating =====");
//    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(accrualSchedule));
//    System.out.println();
//    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(accrualSchedule.createSchedule()));
//    System.out.println();
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(swapLeg));
    System.out.println();
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(expandedLeg));
  }

  //-----------------------------------------------------------------------
  @Test(description = "Demo use of FixedRateSwapLeg")
  public void test_createVanillaFixedVsLibor3mSwap() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USGS);
    BusinessDayAdjustment bdaPreceding = BusinessDayAdjustment.of(PRECEDING, HolidayCalendars.USGS);
    
    FixedRateSwapLeg payLeg = FixedRateSwapLeg.builder()
        .accrualPeriods(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(bda)
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .build())
        .paymentPeriods(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .calculation(FixedRateCalculation.builder()
            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, 100_000_000)))
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
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
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
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.USGS, bdaPreceding))
            .build())
        .build();
    
    SwapTrade trade = SwapTrade.builder()
        .tradeDate(LocalDate.of(2014, 9, 10))
        .leg1(payLeg)
        .leg2(receiveLeg)
        .build();
    
    System.out.println("===== Trade =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(trade));
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(RateIndices.CHF_LIBOR_12M));
  }

//  //-----------------------------------------------------------------------
//  @Test(description = "Demo use of FixedRateSwapLeg realistic values")
//  public void test_standard() {
//    BusinessDayAdjustment bda = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USGS);
//    
//    FixedRateSwapLeg payLeg = FixedRateSwapLeg.builder()
//        .accrualPeriods(PeriodicSchedule.builder()
//            .startDate(LocalDate.of(2011, 11, 14))
//            .endDate(LocalDate.of(2016, 11, 14))
//            .frequency(Frequency.P3M)
//            .businessDayAdjustment(bda)
//            .build())
//        .paymentPeriods(PaymentSchedule.builder()
//            .paymentFrequency(Frequency.P3M)
//            .paymentOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USGS))
//            .build())
//        .calculation(FixedRateCalculation.builder()
//            .notional(NotionalAmount.of(CurrencyAmount.of(Currency.USD, 1_000_000)))
//            .dayCount(DayCounts.THIRTY_360_ISDA)
//            .rate(ValueSchedule.of(0.0125))
//            .build())
//        .build();
//    
//    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(payLeg.toExpanded()));
//  }

}
