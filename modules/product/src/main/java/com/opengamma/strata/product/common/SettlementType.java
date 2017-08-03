/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Flag indicating how a financial instrument is to be settled.
 * <p>
 * Some financial instruments have a choice of settlement, either by cash or by
 * delivering the underlying instrument that was tracked.
 * For example, a swaption might be cash settled or produce an actual interest rate swap.
 */
public enum SettlementType implements NamedEnum {

  /**
   * Cash settlement.
   * <p>
   * Cash amount is paid (by the short party to the long party) at expiry.
   */
  CASH,
  /**
   * Physical delivery.
   * <p>
   * The two parties enter into a new financial instrument at expiry.
   */
  PHYSICAL;

  // helper for name conversions
  private static final EnumNames<SettlementType> NAMES = EnumNames.of(SettlementType.class);

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
  public static SettlementType of(String name) {
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
