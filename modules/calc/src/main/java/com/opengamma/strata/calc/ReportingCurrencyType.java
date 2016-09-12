/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The available types of reporting currency.
 * <p>
 * There are three options - 'Specific', 'Natural' and 'None'.
 */
public enum ReportingCurrencyType {

  /**
   * The specific reporting currency.
   * See {@link ReportingCurrency#of(Currency)}.
   */
  SPECIFIC,
  /**
   * The "natural" reporting currency.
   * See {@link ReportingCurrency#NATURAL}.
   */
  NATURAL,
  /**
   * No currency conversion is to be performed.
   * See {@link ReportingCurrency#NONE}.
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
  public static ReportingCurrencyType of(String uniqueName) {
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
