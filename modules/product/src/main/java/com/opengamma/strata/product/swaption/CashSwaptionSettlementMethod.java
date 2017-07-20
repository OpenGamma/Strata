/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Cash settlement method of cash settled swaptions.
 * <p>
 * Reference: "Swaption Pricing", OpenGamma Documentation 10, Version 1.2, April 2011.
 */
public enum CashSwaptionSettlementMethod implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<CashSwaptionSettlementMethod> NAMES = EnumNames.of(CashSwaptionSettlementMethod.class);

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
  public static CashSwaptionSettlementMethod of(String name) {
    return NAMES.parse(name);
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
