/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Standardized credit default swap conventions.
 */
final class StandardCdsConventions {

  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY_JPTO = JPTO.combinedWith(GBLO_USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);

  /**
   * USD-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'USNY'.
   */
  public static final ImmutableCdsConvention USD_STANDARD = ImmutableCdsConvention.of(
      "USD-STANDARD", USD, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, USNY), DaysAdjustment.ofBusinessDays(3, USNY));

  /**
   * EUR-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'EUTA'.
   */
  public static final ImmutableCdsConvention EUR_STANDARD = ImmutableCdsConvention.of(
      "EUR-STANDARD", EUR, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, EUTA), DaysAdjustment.ofBusinessDays(3, EUTA));

  /**
   * EUR-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'EUTA' and 'GBLO'.
   */
  public static final ImmutableCdsConvention EUR_GB_STANDARD = ImmutableCdsConvention.of(
      "EUR-GB-STANDARD", EUR, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, GBLO_EUTA),
      DaysAdjustment.ofBusinessDays(3, GBLO_EUTA));

  /**
   * GBP-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'GBLO'.
   */
  public static final ImmutableCdsConvention GBP_STANDARD = ImmutableCdsConvention.of(
      "GBP-STANDARD", GBP, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, GBLO), DaysAdjustment.ofBusinessDays(3, GBLO));

  /**
   * GBP-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'GBLO' and 'USNY'.
   */
  public static final ImmutableCdsConvention GBP_US_STANDARD = ImmutableCdsConvention.of(
      "GBP-US-STANDARD", GBP, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, GBLO_USNY),
      DaysAdjustment.ofBusinessDays(3, GBLO_USNY));

  /**
   * JPY-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'JPTO'.
   */
  public static final ImmutableCdsConvention JPY_STANDARD = ImmutableCdsConvention.of(
      "JPY-STANDARD", JPY, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, JPTO), DaysAdjustment.ofBusinessDays(3, JPTO));

  /**
   * JPY-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'JPTO', 'USNY' and 'GBLO'.
   */
  public static final ImmutableCdsConvention JPY_US_GB_STANDARD = ImmutableCdsConvention.of(
      "JPY-US-GB-STANDARD", JPY, ACT_360, P3M, BusinessDayAdjustment.of(FOLLOWING, GBLO_USNY_JPTO),
      DaysAdjustment.ofBusinessDays(3, GBLO_USNY_JPTO));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCdsConventions() {
  }

}
