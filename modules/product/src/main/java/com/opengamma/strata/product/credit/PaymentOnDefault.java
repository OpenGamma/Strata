/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * The payment on default.
 * <p>
 * Whether the accrued premium is paid in the event of a default.
 */
public enum PaymentOnDefault {

  /**
   * The accrued premium.
   * <p>
   * If the credit event happens between coupon dates, the accrued premium is paid. 
   */
  ACCRUED_PREMIUM,
  /**
   * None. 
   * <p>
   * Even if the credit event happens between coupon dates, the accrued premium is not paid.
   */
  NONE;

  //-------------------------------------------------------------------------
  /**
   * Check if the accrued premium is paid
   * 
   * @return true if the accrued premium is paid, false otherwise
   */
  public boolean isAccruedInterest() {
    return this == ACCRUED_PREMIUM;
  }

}
