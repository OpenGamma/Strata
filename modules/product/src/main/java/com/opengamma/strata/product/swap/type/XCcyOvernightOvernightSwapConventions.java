/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard cross-currency overnight-overnight swap conventions.
 */
public final class XCcyOvernightOvernightSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<XCcyOvernightOvernightSwapConvention> ENUM_LOOKUP = 
      ExtendedEnum.of(XCcyOvernightOvernightSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-ESTR-3M-USD-SOFR-3M' swap convention.
   * <p>
   * EUR ESTR 3M v USD SOFR 3M.
   * The spread is on the EUR leg.
   */
  public static final XCcyOvernightOvernightSwapConvention EUR_ESTR_3M_USD_SOFR_3M =
      XCcyOvernightOvernightSwapConvention.of(StandardXCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M.getName());

  /**
   * The 'GBP-SONIA-3M-USD-SOFR-3M' swap convention.
   * <p>
   * GBP SONIA 3M v USD SOFR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyOvernightOvernightSwapConvention GBP_SONIA_3M_USD_SOFR_3M =
      XCcyOvernightOvernightSwapConvention.of(StandardXCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M.getName());

  /**
   * The 'GBP-SONIA-3M-EUR-ESTR-3M' swap convention.
   * <p>
   * GBP SONIA 3M v EUR ESTR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyOvernightOvernightSwapConvention GBP_SONIA_3M_EUR_ESTR_3M =
      XCcyOvernightOvernightSwapConvention.of(StandardXCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M.getName());

  /**
   * The 'JPY-TONA-3M-USD-SOFR-3M' swap convention.
   * <p>
   * JPY TONA 3M v USD SOFR 3M.
   * The spread is on the JPY leg.
   */
  public static final XCcyOvernightOvernightSwapConvention JPY_TONA_3M_USD_SOFR_3M =
      XCcyOvernightOvernightSwapConvention.of(StandardXCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private XCcyOvernightOvernightSwapConventions() {
  }

}
