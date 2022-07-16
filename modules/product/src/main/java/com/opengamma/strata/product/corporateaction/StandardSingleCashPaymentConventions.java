/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

/**
 * Market standard Fixed-Overnight swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
final class StandardSingleCashPaymentConventions {



  public static final SingleCashPaymentConvention CASH_DIVIDEND_MANDATORY =
     makeConvention("CASH_DIVIDEND_MANDATORY");

  //-------------------------------------------------------------------------
  // build conventions
  private static SingleCashPaymentConvention makeConvention(String name) {

    return ImmutableSingleCashPaymentConvention.of(
        name,
        SingleCashPaymentLegConvention.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardSingleCashPaymentConventions() {
  }

}
