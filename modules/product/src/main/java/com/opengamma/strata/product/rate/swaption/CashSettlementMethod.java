/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swaption;

/**
 * Cash settlement method of cash settled swaptions. 
 * <p>
 * Reference: "Swaption Pricing", OpenGamma Documentation 10, Version 1.2, April 2011.
 */
public enum CashSettlementMethod {
  /**
   * The cash price method
   * <p>
   * If exercised, the value of the underlying swap is exchanged at the cash settlement date. 
   */
  CASH_PRICE,
  /**
   * The par yield curve method.
   * <p>
   * The settlement amount is computed with cash-settled annuity using the pre-agreed strike swap rate. 
   */
  PAR_YIELD,
  /**
   * The zero coupon yield method. 
   * <p>
   * The settlement amount is computed with the discount factor based on the agreed zero coupon curve.
   */
  ZERO_COUPON_YIELD;
}
