/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Cash settlement method of cash settled swaptions.
 * <p>
 * Reference: "Swaption Pricing", OpenGamma Documentation 10, Version 1.2, April 2011.
 */
public enum CashSwaptionSettlementMethod {

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

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CashSwaptionSettlementMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

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

}
