/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard term deposit conventions.
 */
public final class TermDepositConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<TermDepositConvention> ENUM_LOOKUP = ExtendedEnum.of(TermDepositConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-Deposit' term deposit convention with T+0 settlement date.
   */
  public static final TermDepositConvention GBP_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.GBP_DEPOSIT.getName());

  /**
   * The 'EUR-Deposit' term deposit convention with T+2 settlement date.
   */
  public static final TermDepositConvention EUR_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_DEPOSIT.getName());
  /**
   * The 'EUR-Deposit-T' term deposit convention with T+0 settlement date, used mainly for O/N deposits.
   */
  public static final TermDepositConvention EUR_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_DEPOSIT_T0.getName());
  /**
   * The 'EUR-Deposit-T' term deposit convention with T+1 settlement date, used mainly for T/N deposits
   */
  public static final TermDepositConvention EUR_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_DEPOSIT_T1.getName());

  /**
   * The 'USD-Deposit' term deposit convention.
   */
  public static final TermDepositConvention USD_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.USD_DEPOSIT.getName());
  /**
   * The 'USD-Deposit-T' term deposit convention with T+0 settlement date, used for O/N deposits.
   */
  public static final TermDepositConvention USD_DEPOSIT_T0 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_DEPOSIT_T0.getName());
  /**
   * The 'USD-Deposit-T' term deposit convention with T+1 settlement date, used for T/N deposits
   */
  public static final TermDepositConvention USD_DEPOSIT_T1 =
      TermDepositConvention.of(StandardTermDepositConventions.USD_DEPOSIT_T1.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private TermDepositConventions() {
  }

}
