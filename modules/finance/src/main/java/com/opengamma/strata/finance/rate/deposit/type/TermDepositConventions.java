/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit.type;

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
   * The 'GBP-Deposit' term deposit convention.
   */
  public static final TermDepositConvention GBP_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.GBP_DEPOSIT.getName());
  /**
   * The 'EUR-Deposit' term deposit convention.
   */
  public static final TermDepositConvention EUR_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.EUR_DEPOSIT.getName());
  /**
   * The 'USD-Deposit' term deposit convention.
   */
  public static final TermDepositConvention USD_DEPOSIT =
      TermDepositConvention.of(StandardTermDepositConventions.USD_DEPOSIT.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private TermDepositConventions() {
  }

}
