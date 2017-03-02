/*
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
   * The 'JPY-LIBOR-1M-LIBOR-6M' swap convention.
   * <p>
   * JPY standard LIBOR 1M vs LIBOR 6M swap.
   * The LIBOR 1M leg pays monthly, the LIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_1M_LIBOR_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M.getName());

  /**
   * The 'JPY-LIBOR-3M-LIBOR-6M' swap convention.
   * <p>
   * JPY standard LIBOR 3M vs LIBOR 6M swap.
   * The LIBOR 3M leg pays quarterly, the LIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_3M_LIBOR_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'JPY-LIBOR-6M-TIBOR-JAPAN-6M' swap convention.
   * <p>
   * JPY standard LIBOR 6M vs TIBOR JAPAN 6M swap.
   * The two legs pay semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_6M_TIBOR_JAPAN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M.getName());

  /**
   * The 'JPY-LIBOR-6M-TIBOR-EUROYEN-6M' swap convention.
   * <p>
   * JPY standard LIBOR 6M vs TIBOR EUROYEN 6M swap.
   * The two legs pay semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_6M_TIBOR_EUROYEN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'JPY-TIBORJ-1M-TIBOR-JAPAN-6M' swap convention.
   * <p>
   * JPY standard TIBOR JAPAN 1M vs TIBOR JAPAN 6M swap.
   * The TIBOR 1M leg pays monthly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M.getName());

  /**
   * The 'JPY-TIBOR-JAPAN-3M-TIBOR-JAPAN-6M' swap convention.
   * <p>
   * JPY standard TIBOR JAPAN 3M vs TIBOR JAPAN 6M swap.
   * The TIBOR 3M leg pays quarterly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'JPY-TIBOR-EUROYEN-1M-TIBOR-EUROYEN-6M' swap convention.
   * <p>
   * JPY standard TIBOR EUROYEN 1M vs TIBOR EUROYEN 6M swap.
   * The TIBOR 1M leg pays monthly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M.getName());

  /**
   * The 'JPY-TIBOR-EUROYEN-3M-TIBOR-EUROYEN-6M' swap convention.
   * <p>
   * JPY standard TIBOR EUROYEN 3M vs TIBOR EUROYEN 6M swap.
   * The TIBOR 3M leg pays quarterly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M =
      IborIborSwapConvention.of(StandardIborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborIborSwapConventions() {
  }

}
