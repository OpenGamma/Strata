/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The payment on default.
 * <p>
 * Whether the accrued premium is paid in the event of a default.
 */
public enum PaymentOnDefault implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<PaymentOnDefault> NAMES = EnumNames.of(PaymentOnDefault.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PaymentOnDefault of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Check if the accrued premium is paid.
   * 
   * @return true if the accrued premium is paid, false otherwise
   */
  public boolean isAccruedInterest() {
    return this == ACCRUED_PREMIUM;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
