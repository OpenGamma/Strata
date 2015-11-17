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

  // EUR with standard spot T+2
  public static final TermDepositConvention EUR_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.EUR,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, EUTA)).expand();
  // EUR with value date T+0; used for O/N
  public static final TermDepositConvention EUR_DEPOSIT_T0 = ImmutableTermDepositConvention.builder()
      .name("EUR-Deposit-T")
      .currency(Currency.EUR)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
      .dayCount(ACT_360).build().expand();
  // EUR with value date T+1; used for T/N
  public static final TermDepositConvention EUR_DEPOSIT_T1 = ImmutableTermDepositConvention.builder()
      .name("EUR-Deposit-T+1")
      .currency(Currency.EUR)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(1, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
      .dayCount(ACT_360).build().expand();

  // USD with standard spot T+2
  public static final TermDepositConvention USD_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.USD,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY))).expand();
  // USD with value date T+0; used for O/N
  public static final TermDepositConvention USD_DEPOSIT_T0 = ImmutableTermDepositConvention.builder()
      .name("USD-Deposit-T")
      .currency(Currency.USD)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)))
      .dayCount(ACT_360).build().expand();
  // USD with value date T+1; used for T/N
  public static final TermDepositConvention USD_DEPOSIT_T1 = ImmutableTermDepositConvention.builder()
      .name("USD-Deposit-T+1")
      .currency(Currency.USD)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(1, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)))
      .dayCount(ACT_360).build().expand();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardTermDepositConventions() {
  }

}
