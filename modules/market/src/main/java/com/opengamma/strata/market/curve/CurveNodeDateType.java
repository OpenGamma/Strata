/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The types of curve node date.
 * <p>
 * This is used to identify how the date of a node should be calculated.
 */
public enum CurveNodeDateType implements NamedEnum {

  /**
   * Defines a fixed date that is externally provided.
   */
  FIXED,
  /**
   * Defines the end date of the trade.
   * This will typically be the last accrual date, but may be any suitable
   * date at the end of the trade.
   */
  END,
  /**
   * Defines the last fixing date referenced in the trade.
   * Used only for instruments referencing an Ibor index.
   */
  LAST_FIXING;

  // helper for name conversions
  private static final EnumNames<CurveNodeDateType> NAMES = EnumNames.of(CurveNodeDateType.class);

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
  public static CurveNodeDateType of(String name) {
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
