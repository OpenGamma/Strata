/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
public final class SingleCashPaymentConventions {

  static final ExtendedEnum<SingleCashPaymentConvention> ENUM_LOOKUP = ExtendedEnum.of(SingleCashPaymentConvention.class);




  public static final SingleCashPaymentConvention CASH_DIVIDEND_MANDATORY =
          SingleCashPaymentConvention.of(StandardSingleCashPaymentConventions.CASH_DIVIDEND_MANDATORY.getName());


  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private SingleCashPaymentConventions() {
  }

}
