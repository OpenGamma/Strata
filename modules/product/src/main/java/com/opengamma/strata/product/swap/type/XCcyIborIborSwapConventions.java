/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard cross-currency Ibor-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class XCcyIborIborSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<XCcyIborIborSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(XCcyIborIborSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-USD-LIBOR-3M' swap convention.
   * <p>
   * EUR EURIBOR 3M v USD LIBOR 3M.
   * The spread is on the EUR leg.
   */
  public static final XCcyIborIborSwapConvention EUR_EURIBOR_3M_USD_LIBOR_3M =
      XCcyIborIborSwapConvention.of(StandardXCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M.getName());

  /**
   * The 'GBP-LIBOR-3M-USD-LIBOR-3M' swap convention.
   * <p>
   * GBP LIBOR 3M v USD LIBOR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyIborIborSwapConvention GBP_LIBOR_3M_USD_LIBOR_3M =
      XCcyIborIborSwapConvention.of(StandardXCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private XCcyIborIborSwapConventions() {
  }

}
