/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The expiry type of an Exchange Traded Derivative (ETD) product.
 * <p>
 * Most ETDs expire monthly, on a date calculated via a formula.
 * Some ETDs expire weekly, or on a specific date, see {@link EtdVariant} for more details.
 */
public enum EtdExpiryType {

  /**
   * The ETD expires once a month on a standardized day.
   */
  MONTHLY,
  /**
   * The ETD expires in a specific week of the month.
   * The week is specified by the date code in {@link EtdVariant}.
   */
  WEEKLY,
  /**
   * The ETD expires on a specified day-of-month.
   * The day-of-month is specified by the date code in {@link EtdVariant}.
   */
  DAILY;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static EtdExpiryType of(String uniqueName) {
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
