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
   * The 'USD-SOFR-3M-FED-FUND-3M' swap convention.
   * <p>
   * USD SOFR v USD Fed Fund 3M swap.
   * <p>
   * The spot date offset is 2 days.
   */
  public static final OvernightOvernightSwapConvention USD_SOFR_3M_FED_FUND_3M =
      OvernightOvernightSwapConvention.of(StandardOvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightOvernightSwapConventions() {
  }

}
