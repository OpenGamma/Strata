/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Flag indicating how a financial instrument is to be settled.
 * <p>
 * Some financial instruments have a choice of settlement, either by cash or by
 * delivering the underlying instrument that was tracked.
 * For example, a swaption might be cash settled or produce an actual interest rate swap.
 */
public enum SettlementType {

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

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SettlementType of(String uniqueName) {
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

}
