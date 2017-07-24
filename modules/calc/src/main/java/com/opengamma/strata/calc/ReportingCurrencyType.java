/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The available types of reporting currency.
 * <p>
 * There are three options - 'Specific', 'Natural' and 'None'.
 */
public enum ReportingCurrencyType implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<ReportingCurrencyType> NAMES = EnumNames.of(ReportingCurrencyType.class);

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
  public static ReportingCurrencyType of(String name) {
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
