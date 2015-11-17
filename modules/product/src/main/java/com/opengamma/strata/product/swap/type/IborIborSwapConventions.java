/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Ibor-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class IborIborSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborIborSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(IborIborSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-LIBOR-6M' swap convention.
   * <p>
   * USD standard LIBOR 3M vs LIBOR 6M swap.
   * The LIBOR 3M leg pays semi-annually with 'Flat' compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_3M_LIBOR_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M.getName());

  /**
   * The 'USD-LIBOR-1M-LIBOR-3M' swap convention.
   * <p>
   * USD standard LIBOR 1M vs LIBOR 3M swap.
   * The LIBOR 1M leg pays quarterly with 'Flat' compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_1M_LIBOR_3M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborIborSwapConventions() {
  }

}
