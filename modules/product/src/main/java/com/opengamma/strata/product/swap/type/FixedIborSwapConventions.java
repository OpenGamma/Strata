/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class FixedIborSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FixedIborSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(FixedIborSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FIXED-6M-LIBOR-3M' swap convention.
   * <p>
   * USD(NY) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_6M_LIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M.getName());

  /**
   * The 'USD-FIXED-1Y-LIBOR-3M' swap convention.
   * <p>
   * USD(London) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_1Y_LIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-FIXED-1Y-EURIBOR-3M' swap convention.
   * <p>
   * EUR(1Y) vanilla fixed vs Euribor 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M.getName());

  /**
   * The 'EUR-FIXED-1Y-EURIBOR-6M' swap convention.
   * <p>
   * EUR(>1Y) vanilla fixed vs Euribor 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_6M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-FIXED-1Y-LIBOR-3M' swap convention.
   * <p>
   * GBP(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_1Y_LIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M.getName());

  /**
   * The 'GBP-FIXED-6M-LIBOR-6M' swap convention.
   * <p>
   * GBP(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_6M_LIBOR_6M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M.getName());

  /**
   * The 'GBP-FIXED-3M-LIBOR-3M' swap convention.
   * <p>
   * GBP(>1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 3 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_3M_LIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'CHF-FIXED-1Y-LIBOR-3M' swap convention.
   * <p>
   * CHF(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M.getName());

  /**
   * The 'CHF-FIXED-1Y-LIBOR-6M' swap convention.
   * <p>
   * CHF(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_6M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'JPY-FIXED-6M-TIBOR-JAPAN-3M' swap convention.
   * <p>
   * JPY(Tibor) vanilla fixed vs Tibor 3M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_TIBORJ_3M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M.getName());

  /**
   * The 'JPY-FIXED-6M-LIBOR-6M' swap convention.
   * <p>
   * JPY(LIBOR) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_LIBOR_6M =
      FixedIborSwapConvention.of(StandardFixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedIborSwapConventions() {
  }

}
