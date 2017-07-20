/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The accrual start for credit default swaps.
 * <p>
 * The accrual start is the next day or the previous IMM date.
 */
public enum AccrualStart implements NamedEnum {

  /**
   * The accrual starts on T+1, i.e., the next day.
   */
  NEXT_DAY,

  /**
   * The accrual starts on the previous IMM date.
   * <p>
   * The IMM date must be computed based on {@link CdsImmDateLogic}.
   */
  IMM_DATE;

  // helper for name conversions
  private static final EnumNames<AccrualStart> NAMES = EnumNames.of(AccrualStart.class);

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
  public static AccrualStart of(String name) {
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
