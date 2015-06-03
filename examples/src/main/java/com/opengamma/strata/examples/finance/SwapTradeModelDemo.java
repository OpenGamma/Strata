/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.time.LocalDate;

import org.joda.beans.ser.JodaBeanSer;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.FixingRelativeTo;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentRelativeTo;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Demonstrate use of the API for interest rate swaps.
 * <p>
 * This class exists for demonstration purposes to aid with understanding interest rate swaps.
 * It is not intended to be used in a production environment.
 */
public class SwapTradeModelDemo {

  /**
   * Launch demo, no arguments needed.
   * 
   * @param args  no arguments needed
   */
  public static void main(String[] args) {
    SwapTradeModelDemo demo = new SwapTradeModelDemo();
    demo.fixedSwapLeg();
    demo.floatingSwapLeg();
    demo.vanillaFixedVsLibor3mSwap();
  }

  //-----------------------------------------------------------------------
  public void fixedSwapLeg() {
    // a PeriodicSchedule generates a schedule of accrual periods
    // - interest is accrued every 3 months from 2014-02-12 to 2014-07-31
    // - accrual period dates are adjusted "modified following" using the "GBLO" holiday calendar
    // - there will be a long initial stub
    // - the regular accrual period dates will be at the end-of-month
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 2, 12))
        .endDate(LocalDate.of(2016, 7, 31))
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
        .frequency(Frequency.P3M)
        .stubConvention(StubConvention.LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .build();
    // a PaymentSchedule generates a schedule of payment periods, based on the accrual schedule
    // - payments are every 6 months
    // - payments are 2 business days after the end of the period
    // - straight compounding is used (the payments are less frequent than the accrual, so compounding occurs)
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P6M)
        .paymentRelativeTo(PaymentRelativeTo.PERIOD_END)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.GBLO))
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    // a NotionalSchedule generates a schedule of notional amounts, based on the payment schedule
    // - in this simple case the notional is 1 million GBP and does not change
    NotionalSchedule notionalSchedule = NotionalSchedule.of(Currency.GBP, 1_000_000);
    // a RateCalculationSwapLeg can represent a fixed or floating swap leg
    // - a FixedRateCalculation is used to represent a fixed rate
    // - the "Act/Act ISDA" day count is used
    // - the rate starts at 0.8% and reduces to 0.7%
    RateCalculationSwapLeg swapLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(FixedRateCalculation.builder()
            .dayCount(DayCounts.ACT_ACT_ISDA)
            .rate(ValueSchedule.of(
                0.008,
                ValueStep.ofAbsoluteAmount(LocalDate.of(2015, 1, 31), 0.007)))
            .build())
        .build();
    // an ExpandedSwapLeg has all the dates of the cash flows
    // it remains valid so long as the holiday calendar does not change 
    ExpandedSwapLeg expandedLeg = swapLeg.expand();

    System.out.println("===== Fixed =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(swapLeg));
    System.out.println();
    System.out.println("===== Fixed expanded =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(expandedLeg));
    System.out.println();
  }

  //-----------------------------------------------------------------------
  public void floatingSwapLeg() {
    // a PeriodicSchedule generates a schedule of accrual periods
    // - interest is accrued every 6 months from 2014-02-12 to 2014-07-31
    // - accrual period dates are adjusted "modified following" using the "GBLO" holiday calendar
    // - there will be a long initial stub
    // - the regular accrual period dates will be at the end-of-month
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 2, 12))
        .endDate(LocalDate.of(2016, 7, 31))
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
        .frequency(Frequency.P6M)
        .stubConvention(StubConvention.LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .build();
    // a PaymentSchedule generates a schedule of payment periods, based on the accrual schedule
    // - payments are every 6 months
    // - payments are 2 business days after the end of the period
    // - no compounding is needed as the payment schedule matches the accrual schedule
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P6M)
        .paymentRelativeTo(PaymentRelativeTo.PERIOD_END)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.GBLO))
        .build();
    // a NotionalSchedule generates a schedule of notional amounts, based on the payment schedule
    // - in this simple case the notional is 1 million GBP and does not change
    NotionalSchedule notionalSchedule = NotionalSchedule.of(Currency.GBP, 1_000_000);
    // a RateCalculationSwapLeg can represent a fixed or floating swap leg
    // - an IborRateCalculation is used to represent a floating IBOR-like rate
    // - the "Act/Act ISDA" day count is used
    // - the index is GBP LIBOR 6M
    // - fixing is 2 days before the start of the period using the "GBLO" holiday calendar
    RateCalculationSwapLeg swapLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(IborRateCalculation.builder()
            .dayCount(DayCounts.ACT_ACT_ISDA)
            .index(IborIndices.GBP_LIBOR_6M)
            .fixingRelativeTo(FixingRelativeTo.PERIOD_START)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.GBLO))
            .build())
        .build();
    // an ExpandedSwapLeg has all the dates of the cash flows
    // it remains valid so long as the holiday calendar does not change 
    ExpandedSwapLeg expandedLeg = swapLeg.expand();

    System.out.println("===== Floating =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(swapLeg));
    System.out.println();
    System.out.println("===== Floating expanded =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(expandedLeg));
    System.out.println();
  }

  //-----------------------------------------------------------------------
  public void vanillaFixedVsLibor3mSwap() {
    // we are paying a fixed rate every 3 months at 1.5% with a 100 million notional
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(Currency.USD)
            .amount(ValueSchedule.of(100_000_000))
            .build())
        .calculation(FixedRateCalculation.builder()
            .dayCount(DayCounts.THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    // we are receiving USD LIBOR 3M every 3 months with a 100 million notional
    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(Currency.USD)
            .amount(ValueSchedule.of(100_000_000))
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .index(IborIndices.USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.USNY))
            .build())
        .build();
    // a SwapTrade combines the two legs
    SwapTrade trade = SwapTrade.builder()
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("OG-Trade", "1"))
            .tradeDate(LocalDate.of(2014, 9, 10))
            .build())
        .product(Swap.of(payLeg, receiveLeg))
        .build();

    System.out.println("===== Vanilla fixed vs Libor3m =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(trade));
    System.out.println();
    System.out.println("===== Vanilla fixed vs Libor3m pay leg =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(payLeg.expand()));
    System.out.println();
    System.out.println("===== Vanilla fixed vs Libor3m receive leg =====");
    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(receiveLeg.expand()));
    System.out.println();
  }

}
