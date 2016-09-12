/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;

/**
 * Standard Term Deposit implementations.
 * <p>
 * See {@link TermDepositConventions} for the description of each.
 */
final class StandardTermDepositConventions {

  // GBP with standard spot T+0
  public static final TermDepositConvention GBP_DEPOSIT_T0 = ImmutableTermDepositConvention.of(
      "GBP-Deposit-T0",
      Currency.GBP,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
      ACT_365F,
      DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));
  // GBP Following - Used for ON and week tenors
  public static final TermDepositConvention GBP_SHORT_DEPOSIT_T0 = ImmutableTermDepositConvention.of(
      "GBP-ShortDeposit-T0",
      Currency.GBP,
      BusinessDayAdjustment.of(FOLLOWING, GBLO),
      ACT_365F,
      DaysAdjustment.ofBusinessDays(0, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO)));
  // GBP for T+1
  public static final TermDepositConvention GBP_SHORT_DEPOSIT_T1 = ImmutableTermDepositConvention.of(
      "GBP-ShortDeposit-T1",
      Currency.GBP,
      BusinessDayAdjustment.of(FOLLOWING, GBLO),
      ACT_365F,
      DaysAdjustment.ofBusinessDays(1, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO)));

  // EUR with standard spot T+2
  public static final TermDepositConvention EUR_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "EUR-Deposit-T2",
      Currency.EUR,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, EUTA));
  // EUR with value date T+0; used for O/N
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T0 = ImmutableTermDepositConvention.of(
      "EUR-ShortDeposit-T0",
      Currency.EUR,
      BusinessDayAdjustment.of(FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(0, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)));
  // EUR with value date T+1; used for T/N
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T1 = ImmutableTermDepositConvention.of(
      "EUR-ShortDeposit-T1",
      Currency.EUR,
      BusinessDayAdjustment.of(FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(1, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)));
  // EUR with standard spot T+2 for week tenors
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "EUR-ShortDeposit-T2",
      Currency.EUR,
      BusinessDayAdjustment.of(FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)));

  // USD with standard spot T+2
  public static final TermDepositConvention USD_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "USD-Deposit-T2",
      Currency.USD,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));
  // USD with value date T+0; used for O/N
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T0 = ImmutableTermDepositConvention.of(
      "USD-ShortDeposit-T0",
      Currency.USD,
      BusinessDayAdjustment.of(FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(0, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));
  // USD with value date T+1; used for T/N
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T1 = ImmutableTermDepositConvention.of(
      "USD-ShortDeposit-T1",
      Currency.USD,
      BusinessDayAdjustment.of(FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(1, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));
  // USD with standard spot T+2 for week tenors
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "USD-ShortDeposit-T2",
      Currency.USD,
      BusinessDayAdjustment.of(FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));

  // CHF with standard spot T+2
  public static final TermDepositConvention CHF_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "CHF-Deposit-T2",
      Currency.CHF,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CHZU),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, CHZU));
  // CHF with value date T+0; used for O/N
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T0 = ImmutableTermDepositConvention.of(
      "CHF-ShortDeposit-T0",
      Currency.CHF,
      BusinessDayAdjustment.of(FOLLOWING, CHZU),
      ACT_360,
      DaysAdjustment.ofBusinessDays(0, CHZU, BusinessDayAdjustment.of(FOLLOWING, CHZU)));
  // CHF with value date T+1; used for T/N
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T1 = ImmutableTermDepositConvention.of(
      "CHF-ShortDeposit-T1",
      Currency.CHF,
      BusinessDayAdjustment.of(FOLLOWING, CHZU),
      ACT_360,
      DaysAdjustment.ofBusinessDays(1, CHZU, BusinessDayAdjustment.of(FOLLOWING, CHZU)));
  // CHF with standard spot T+2 for week tenors
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T2 = ImmutableTermDepositConvention.of(
      "CHF-ShortDeposit-T2",
      Currency.CHF,
      BusinessDayAdjustment.of(FOLLOWING, CHZU),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, CHZU));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardTermDepositConventions() {
  }

}
