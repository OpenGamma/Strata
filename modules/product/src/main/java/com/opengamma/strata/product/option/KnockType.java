/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The knock type of barrier event.
 * <p>
 * This defines the knock type of {@link Barrier}.
 */
public enum KnockType implements NamedEnum {

  /**
   * Knock-in 
   */
  KNOCK_IN,
  /**
   * Knock-out 
   */
  KNOCK_OUT;

  // helper for name conversions
  private static final EnumNames<KnockType> NAMES = EnumNames.of(KnockType.class);

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
  public static KnockType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Knock-in'.
   * 
   * @return true if knock-in, false if knock-out
   */
  public boolean isKnockIn() {
    return this == KNOCK_IN;
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
