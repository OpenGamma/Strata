/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.JPY_TONAR;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.COMPOUNDED;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Market standard Fixed-Overnight swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardFixedOvernightSwapConventions {

  /**
   * USD fixed vs Fed Fund OIS swap for terms less than or equal to one year.
   * <p>
   * Both legs pay once at the end and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_TERM_FED_FUND_OIS =
      makeConvention("USD-FIXED-TERM-FED-FUND-OIS", USD_FED_FUND, ACT_360, TERM, 2, 2);

  /**
   * USD fixed vs Fed Fund OIS swap for terms greater than one year.
   * <p>
   * Both legs pay annually and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_1Y_FED_FUND_OIS =
      makeConvention("USD-FIXED-1Y-FED-FUND-OIS", USD_FED_FUND, ACT_360, P12M, 2, 2);

  //-------------------------------------------------------------------------
  /**
   * EUR fixed vs EONIA OIS swap for terms less than or equal to one year.
   * <p>
   * Both legs pay once at the end and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_TERM_EONIA_OIS =
      makeConvention("EUR-FIXED-TERM-EONIA-OIS", EUR_EONIA, ACT_360, TERM, 1, 2);

  /**
   * EUR fixed vs EONIA OIS swap for terms greater than one year.
   * <p>
   * Both legs pay annually and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_1Y_EONIA_OIS =
      makeConvention("EUR-FIXED-1Y-EONIA-OIS", EUR_EONIA, ACT_360, P12M, 1, 2);

  //-------------------------------------------------------------------------
  /**
   * GBP fixed vs SONIA OIS swap for terms less than or equal to one year.
   * <p>
   * Both legs pay once at the end and use day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_TERM_SONIA_OIS =
      makeConvention("GBP-FIXED-TERM-SONIA-OIS", GBP_SONIA, ACT_365F, TERM, 0, 0);

  /**
   * GBP fixed vs SONIA OIS swap for terms greater than one year.
   * <p>
   * Both legs pay annually and use day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_1Y_SONIA_OIS =
      makeConvention("GBP-FIXED-1Y-SONIA-OIS", GBP_SONIA, ACT_365F, P12M, 0, 0);

  //-------------------------------------------------------------------------
  /**
   * JPY fixed vs TONAR OIS swap for terms less than or equal to one year.
   * <p>
   * Both legs pay once at the end and use day count 'Act/365F'.
   * The spot date offset is 2 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention JPY_FIXED_TERM_TONAR_OIS =
      makeConvention("JPY-FIXED-TERM-TONAR-OIS", JPY_TONAR, ACT_365F, TERM, 0, 0);

  /**
   * JPY fixed vs TONAR OIS swap for terms greater than one year.
   * <p>
   * Both legs pay annually and use day count 'Act/365F'.
   * The spot date offset is 2 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention JPY_FIXED_1Y_TONAR_OIS =
      makeConvention("JPY-FIXED-1Y-TONAR-OIS", JPY_TONAR, ACT_365F, P12M, 0, 2);

  //-------------------------------------------------------------------------
  // build conventions
  private static FixedOvernightSwapConvention makeConvention(
      String name,
      OvernightIndex index,
      DayCount dayCount,
      Frequency frequency,
      int paymentLag,
      int spotLag) {

    HolidayCalendarId calendar = index.getFixingCalendar();
    DaysAdjustment paymentDateOffset = DaysAdjustment.ofBusinessDays(paymentLag, calendar);
    DaysAdjustment spotDateOffset = DaysAdjustment.ofBusinessDays(spotLag, calendar);
    return ImmutableFixedOvernightSwapConvention.of(
        name,
        FixedRateSwapLegConvention.builder()
            .currency(index.getCurrency())
            .dayCount(dayCount)
            .accrualFrequency(frequency)
            .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, calendar))
            .paymentFrequency(frequency)
            .paymentDateOffset(paymentDateOffset)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build(),
        OvernightRateSwapLegConvention.builder()
            .index(index)
            .accrualMethod(COMPOUNDED)
            .accrualFrequency(frequency)
            .paymentFrequency(frequency)
            .paymentDateOffset(paymentDateOffset)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build(),
        spotDateOffset);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFixedOvernightSwapConventions() {
  }

}
