/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public final class OvernightIborSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightIborSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(OvernightIborSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FED-FUND-AA-LIBOR-3M' swap convention.
   * <p>
   * USD Fed Fund Arithmetic Average 3M v Libor 3M swap.
   * Both legs use day count 'Act/360'.
   * The spot date offset is 2 days, the rate cut-off period is 2 days.
   */
  public static final OvernightIborSwapConvention USD_FED_FUND_AA_LIBOR_3M =
      OvernightIborSwapConvention.of(StandardOvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M.getName());

  /**
   * The 'GBP-SONIA-OIS-1Y-LIBOR-3M' swap convention.
   * <p>
   * GBP Sonia compounded 1Y v LIBOR 3M .
   * Both legs use day count 'Act/365F'.
   * The spot date offset is 0 days and payment offset is 0 days.
   */
  public static final OvernightIborSwapConvention GBP_SONIA_OIS_1Y_LIBOR_3M =
      OvernightIborSwapConvention.of(StandardOvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightIborSwapConventions() {
  }

}
