/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard term deposit conventions.
 * <p>
 * The conventions form two groups, those typically used for deposits of one month
 * and over and those for deposits of less than one month, which have "Short" in the name.
 * <p>
 * The conventions also differ by spot date. Most currencies have a T+2 spot date, where
 * the start date is two days after the trade date.
 * There are special cases for trades that have a T+0 or T+1 convention.
 * The name of each convention includes "T0", "T1" or "T2" two indicate the spot date.
 */
public final class TermDepositConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<TermDepositConvention> ENUM_LOOKUP = ExtendedEnum.of(TermDepositConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-Deposit-T0' term deposit convention with T+0 settlement date.
   * This has the modified following business day convention and is typically used
   * for deposits of one month and over.
   */
  public static final TermDepositConvention GBP_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.GBP_DEPOSIT_T0.getName());
  /**
   * The 'GBP-ShortDeposit-T0' term deposit convention with T+0 settlement date.
   * This has the following business day convention and is typically used for O/N and deposits up to one month.
   */
  public static final TermDepositConvention GBP_SHORT_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.GBP_SHORT_DEPOSIT_T0.getName());
  /**
   * The 'GBP-ShortDeposit-T1' term deposit convention with T+1 settlement date.
   * This has the following business day convention and is typically used for T/N.
   */
  public static final TermDepositConvention GBP_SHORT_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.GBP_SHORT_DEPOSIT_T1.getName());

  /**
   * The 'EUR-Deposit-T2' term deposit convention with T+2 settlement date.
   * This has the modified following business day convention and is typically used
   * for deposits of one month and over.
   */
  public static final TermDepositConvention EUR_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_DEPOSIT_T2.getName());
  /**
   * The 'EUR-ShortDeposit-T0' term deposit convention with T+0 settlement date.
   * This has the following business day convention and is typically used for O/N.
   */
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_SHORT_DEPOSIT_T0.getName());
  /**
   * The 'EUR-ShortDeposit-T1' term deposit convention with T+1 settlement date
   * This has the following business day convention and is typically used for T/N.
   */
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_SHORT_DEPOSIT_T1.getName());
  /**
   * The 'EUR-ShortDeposit-T2' term deposit convention with T+2 settlement date
   * This has the following business day convention and is typically used for deposits up to one month.
   */
  public static final TermDepositConvention EUR_SHORT_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_SHORT_DEPOSIT_T2.getName());

  /**
   * The 'USD-Deposit-T2' term deposit convention with T+2 settlement date.
   * This has the modified following business day convention and is typically used
   * for deposits of one month and over.
   */
  public static final TermDepositConvention USD_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_DEPOSIT_T2.getName());
  /**
   * The 'USD-ShortDeposit-T0' term deposit convention with T+0 settlement date.
   * This has the following business day convention and is typically used for O/N.
   */
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_SHORT_DEPOSIT_T0.getName());
  /**
   * The 'USD-ShortDeposit-T1' term deposit convention with T+1 settlement date
   * This has the following business day convention and is typically used for T/N.
   */
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_SHORT_DEPOSIT_T1.getName());
  /**
   * The 'USD-ShortDeposit-T2' term deposit convention with T+2 settlement date
   * This has the following business day convention and is typically used for deposits up to one month.
   */
  public static final TermDepositConvention USD_SHORT_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_SHORT_DEPOSIT_T2.getName());

  /**
   * The 'CHF-Deposit-T2' term deposit convention with T+2 settlement date.
   * This has the modified following business day convention and is typically used
   * for deposits of one month and over.
   */
  public static final TermDepositConvention CHF_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.CHF_DEPOSIT_T2.getName());
  /**
   * The 'CHF-ShortDeposit-T0' term deposit convention with T+0 settlement date.
   * This has the following business day convention and is typically used for O/N.
   */
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.CHF_SHORT_DEPOSIT_T0.getName());
  /**
   * The 'CHF-ShortDeposit-T1' term deposit convention with T+1 settlement date
   * This has the following business day convention and is typically used for T/N.
   */
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.CHF_SHORT_DEPOSIT_T1.getName());
  /**
   * The 'CHF-ShortDeposit-T2' term deposit convention with T+2 settlement date
   * This has the following business day convention and is typically used for deposits up to one month.
   */
  public static final TermDepositConvention CHF_SHORT_DEPOSIT_T2 =
      TermDepositConvention.of(StandardTermDepositConventions.CHF_SHORT_DEPOSIT_T2.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private TermDepositConventions() {
  }

}
