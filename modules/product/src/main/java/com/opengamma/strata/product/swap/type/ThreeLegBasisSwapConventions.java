/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard three leg basis swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class ThreeLegBasisSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<ThreeLegBasisSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(ThreeLegBasisSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-FIXED-1Y-EURIBOR-3M-EURIBOR-6M' swap convention.
   * <p>
   * EUR three leg basis swap of fixed, Euribor 3M and Euribor 6M.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final ThreeLegBasisSwapConvention EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M =
      ThreeLegBasisSwapConvention.of(StandardThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private ThreeLegBasisSwapConventions() {
  }

}
