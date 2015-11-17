/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Fixed-Overnight swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class FixedOvernightSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FixedOvernightSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(FixedOvernightSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FIXED-TERM-FED-FUND-OIS' swap convention.
   * <p>
   * USD fixed vs Fed Fund OIS swap for terms less than or equal to one year.
   * Both legs pay once at the end and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_TERM_FED_FUND_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS.getName());

  /**
   * The 'USD-FIXED-1Y-FED-FUND-OIS' swap convention.
   * <p>
   * USD fixed vs Fed Fund OIS swap for terms greater than one year.
   * Both legs pay annually and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 2 days.
   */
  public static final FixedOvernightSwapConvention USD_FIXED_1Y_FED_FUND_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-FIXED-TERM-EONIA-OIS' swap convention.
   * <p>
   * EUR fixed vs EONIA OIS swap for terms less than or equal to one year.
   * Both legs pay once at the end and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_TERM_EONIA_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS.getName());

  /**
   * The 'EUR-FIXED-1Y-EONIA_OIS' swap convention.
   * <p>
   * EUR fixed vs EONIA OIS swap for terms greater than one year.
   * Both legs pay annually and use day count 'Act/360'.
   * The spot date offset is 2 days and the payment date offset is 1 day.
   */
  public static final FixedOvernightSwapConvention EUR_FIXED_1Y_EONIA_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-FIXED-TERM-SONIA-OIS' swap convention.
   * <p>
   * GBP fixed vs SONIA OIS swap for terms less than or equal to one year.
   * Both legs pay once at the end and use day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_TERM_SONIA_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS.getName());

  /**
   * The 'GBP-FIXED-1Y-SONIA-OIS' swap convention.
   * <p>
   * GBP fixed vs SONIA OIS swap for terms greater than one year.
   * Both legs pay annually and use day count 'Act/365F'.
   * The spot date offset is 0 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention GBP_FIXED_1Y_SONIA_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'JPY_FIXED_TERM_TONAR-OIS' swap convention.
   * <p>
   * JPY fixed vs TONAR OIS swap for terms less than or equal to one year.
   * Both legs pay once at the end and use day count 'Act/365F'.
   * The spot date offset is 2 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention JPY_FIXED_TERM_TONAR_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS.getName());

  /**
   * The 'JPY-FIXED-1Y-TONAR-OIS' swap convention.
   * <p>
   * JPY fixed vs TONAR OIS swap for terms greater than one year.
   * Both legs pay annually and use day count 'Act/365F'.
   * The spot date offset is 2 days and there is no payment date offset.
   */
  public static final FixedOvernightSwapConvention JPY_FIXED_1Y_TONAR_OIS =
      FixedOvernightSwapConvention.of(StandardFixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedOvernightSwapConventions() {
  }

}
