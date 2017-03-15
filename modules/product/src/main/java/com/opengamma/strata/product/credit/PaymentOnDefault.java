/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

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
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PaymentOnDefault of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

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
