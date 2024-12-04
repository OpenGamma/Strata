/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Overnight-Overnight swap conventions.
 */
public final class OvernightOvernightSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightOvernightSwapConvention> ENUM_LOOKUP =
      ExtendedEnum.of(OvernightOvernightSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FED-FUND-AA-LIBOR-3M' swap convention.
   * <p>
   * USD Fed Fund Arithmetic Average 3M v Libor 3M swap.
   * Both legs use day count 'Act/360'.
   * The spot date offset is 2 days, the rate cut-off period is 2 days.
   */
  public static final OvernightOvernightSwapConvention USD_FED_FUND_SOFR_3M =
      OvernightOvernightSwapConvention.of(StandardOvernightOvernightSwapConventions.USD_FED_FUND_SOFR_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightOvernightSwapConventions() {
  }

}
