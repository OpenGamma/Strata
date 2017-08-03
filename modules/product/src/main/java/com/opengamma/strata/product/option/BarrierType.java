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
 * The barrier type of barrier event.
 * <p>
 * This defines the barrier type of {@link Barrier}.
 */
public enum BarrierType implements NamedEnum {

  /**
   * Down 
   */
  DOWN,
  /**
   * Up 
   */
  UP;

  // helper for name conversions
  private static final EnumNames<BarrierType> NAMES = EnumNames.of(BarrierType.class);

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
  public static BarrierType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Down'.
   * 
   * @return true if down, false if up
   */
  public boolean isDown() {
    return this == DOWN;
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
