/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;

/**
 * Factory methods for market standard conventions
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public class FixedOvernightSwapConventions {

  /**
   * USD fixed vs Fed Fund OIS swap for terms less than or equal to one year.
   * <p>
   * The fixed leg pays once at the end with day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_TERM_FED_FUND_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(USD, ACT_360, TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)),
          OvernightRateSwapLegConvention.of(USD_FED_FUND, TERM, 2),
          DaysAdjustment.ofBusinessDays(2, USNY));

  /**
   * USD fixed vs Fed Fund OIS swap for terms greater than one year.
   * <p>
   * The fixed leg pays annually with day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_1Y_FED_FUND_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(USD, ACT_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)),
          OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 2),
          DaysAdjustment.ofBusinessDays(2, USNY));

  //-------------------------------------------------------------------------
  /**
   * EUR fixed vs EONIA OIS swap for terms less than or equal to one year.
   * <p>
   * The fixed leg pays once at the end with day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_TERM_EONIA_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(EUR, ACT_360, TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          OvernightRateSwapLegConvention.of(EUR_EONIA, TERM, 1),
          DaysAdjustment.ofBusinessDays(2, EUTA));

  /**
   * EUR fixed vs EONIA OIS swap for terms greater than one year.
   * <p>
   * The fixed leg pays once at the end with day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_1Y_EONIA_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(EUR, ACT_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          OvernightRateSwapLegConvention.of(EUR_EONIA, P12M, 1),
          DaysAdjustment.ofBusinessDays(2, EUTA));

  //-------------------------------------------------------------------------
  /**
   * GBP fixed vs SONIA OIS swap for terms less than or equal to one year.
   * <p>
   * The fixed leg pays once at the end with day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_TERM_SONIA_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(GBP, ACT_365F, TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          OvernightRateSwapLegConvention.of(GBP_SONIA, TERM, 0),
          DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));

  /**
   * GBP fixed vs SONIA OIS swap for terms greater than one year.
   * <p>
   * The fixed leg pays once at the end with day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_1Y_SONIA_OIS =
      FixedOvernightSwapConvention.of(
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 0),
          DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedOvernightSwapConventions() {
  }

}
