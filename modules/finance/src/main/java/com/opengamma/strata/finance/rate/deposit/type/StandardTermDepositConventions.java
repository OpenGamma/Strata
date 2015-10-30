/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;

/**
 * Standard Term Deposit implementations.
 * <p>
 * See {@link TermDepositConventions} for the description of each.
 */
final class StandardTermDepositConventions {

  // GBP
  public static final TermDepositConvention GBP_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.GBP,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
      ACT_365F,
      DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO))).expand();
  // EUR
  public static final TermDepositConvention EUR_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.EUR,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, EUTA)).expand();
  // USD
  public static final TermDepositConvention USD_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.USD,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO.combineWith(USNY)))).expand();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardTermDepositConventions() {
  }

}
