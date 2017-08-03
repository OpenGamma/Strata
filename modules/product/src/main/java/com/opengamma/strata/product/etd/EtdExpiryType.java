/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The expiry type of an Exchange Traded Derivative (ETD) product.
 * <p>
 * Most ETDs expire monthly, on a date calculated via a formula.
 * Some ETDs expire weekly, or on a specific date, see {@link EtdVariant} for more details.
 */
public enum EtdExpiryType implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<EtdExpiryType> NAMES = EnumNames.of(EtdExpiryType.class);

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
  public static EtdExpiryType of(String name) {
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
