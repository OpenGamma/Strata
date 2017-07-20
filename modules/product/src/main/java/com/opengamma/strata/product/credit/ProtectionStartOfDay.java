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
 * The protection start of the day.
 * <p>
 * When the protection starts on the start date.
 */
public enum ProtectionStartOfDay implements NamedEnum {

  /**
   * Beginning of the start day. 
   * <p>
   * The protection starts at the beginning of the day. 
   */
  BEGINNING,
  /**
   * None.
   * <p>
   * The protection start is not specified. 
   * The CDS is priced based on the default date logic in respective model implementation.
   */
  NONE;

  // helper for name conversions
  private static final EnumNames<ProtectionStartOfDay> NAMES = EnumNames.of(ProtectionStartOfDay.class);

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
  public static ProtectionStartOfDay of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Check if the type is 'Beginning'.
   * 
   * @return true if beginning, false otherwise
   */
  public boolean isBeginning() {
    return this == BEGINNING;
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
