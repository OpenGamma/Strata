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

  // GBP
  public static final TermDepositConvention GBP_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.GBP,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
      ACT_365F,
      DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));
  // GBP Following - Used for ON, TN and week tenors
  public static final TermDepositConvention GBP_DEPOSIT_FOL = ImmutableTermDepositConvention.builder()
      .name("GBP-Deposit-Following")
      .currency(Currency.GBP)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO)))
      .dayCount(ACT_365F).build();

  // EUR with standard spot T+2
  public static final TermDepositConvention EUR_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.EUR,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, EUTA));
  // EUR with standard spot T+2 for week tenors
  public static final TermDepositConvention EUR_DEPOSIT_FOL = ImmutableTermDepositConvention.builder()
      .name("EUR-Deposit-Following")
      .currency(Currency.EUR)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
      .dayCount(ACT_360).build();
  // EUR with value date T+0; used for O/N
  public static final TermDepositConvention EUR_DEPOSIT_T0 = ImmutableTermDepositConvention.builder()
      .name("EUR-Deposit-T")
      .currency(Currency.EUR)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
      .dayCount(ACT_360).build();
  // EUR with value date T+1; used for T/N
  public static final TermDepositConvention EUR_DEPOSIT_T1 = ImmutableTermDepositConvention.builder()
      .name("EUR-Deposit-T+1")
      .currency(Currency.EUR)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(1, EUTA, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
      .dayCount(ACT_360).build();

  // USD with standard spot T+2
  public static final TermDepositConvention USD_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.USD,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));
  // USD with standard spot T+2 for week tenors
  public static final TermDepositConvention USD_DEPOSIT_FOL = ImmutableTermDepositConvention.of(
      Currency.USD,
      BusinessDayAdjustment.of(FOLLOWING, USNY),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)));
  // USD with value date T+0; used for O/N
  public static final TermDepositConvention USD_DEPOSIT_T0 = ImmutableTermDepositConvention.builder()
      .name("USD-Deposit-T")
      .currency(Currency.USD)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)))
      .dayCount(ACT_360).build();
  // USD with value date T+1; used for T/N
  public static final TermDepositConvention USD_DEPOSIT_T1 = ImmutableTermDepositConvention.builder()
      .name("USD-Deposit-T+1")
      .currency(Currency.USD)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(1, USNY, BusinessDayAdjustment.of(FOLLOWING, USNY)))
      .dayCount(ACT_360).build();

  // CHF with standard spot T+2
  public static final TermDepositConvention CHF_DEPOSIT = ImmutableTermDepositConvention.of(
      Currency.CHF,
      BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CHZU),
      ACT_360,
      DaysAdjustment.ofBusinessDays(2, CHZU));
  // CHF with standard spot T+2 for week tenors
  public static final TermDepositConvention CHF_DEPOSIT_FOL = ImmutableTermDepositConvention.builder()
      .name("CHF-Deposit-Following")
      .currency(Currency.CHF)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CHZU))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(2, CHZU, BusinessDayAdjustment.of(FOLLOWING, CHZU)))
      .dayCount(ACT_360).build();
  // CHF with value date T+0; used for O/N
  public static final TermDepositConvention CHF_DEPOSIT_T0 = ImmutableTermDepositConvention.builder()
      .name("CHF-Deposit-T")
      .currency(Currency.CHF)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CHZU))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(0, CHZU, BusinessDayAdjustment.of(FOLLOWING, CHZU)))
      .dayCount(ACT_360).build();
  // CHF with value date T+1; used for T/N
  public static final TermDepositConvention CHF_DEPOSIT_T1 = ImmutableTermDepositConvention.builder()
      .name("CHF-Deposit-T+1")
      .currency(Currency.CHF)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CHZU))
      .spotDateOffset(DaysAdjustment.ofBusinessDays(1, CHZU, BusinessDayAdjustment.of(FOLLOWING, CHZU)))
      .dayCount(ACT_360).build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardTermDepositConventions() {
  }

}
