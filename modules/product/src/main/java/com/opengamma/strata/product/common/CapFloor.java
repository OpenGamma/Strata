/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import java.util.Locale;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Flag indicating whether a financial instrument is "cap" or a "floor".
 * <p>
 * Specifies whether the financial instrument is cap or a floor.
 * For example, in an interest rate cap the buyer receives payments at the end of each period
 * in which the interest rate exceeds the agreed strike price.
 */
public enum CapFloor implements NamedEnum {
  /**
   * Cap.
   */
  CAP,

  /**
   * Floor.
   */
  FLOOR;

  // helper for name conversions
  private static final EnumNames<CapFloor> NAMES = EnumNames.of(CapFloor.class);

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
  public static CapFloor of(String name) {
    return NAMES.parse(name.toUpperCase(Locale.ENGLISH));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Cap'.
   *
   * @return true if cap, false if floor
   */
  public boolean isCap() {
    return this == CAP;
  }

  /**
   * Checks if the type is 'Floor'.
   *
   * @return true if floor, false if cap
   */
  public boolean isFloor() {
    return this == FLOOR;
  }

  //-------------------------------------------------------------------------
  /**
   * Supplies the opposite of this value.
   *
   * @return the opposite value
   */
  public CapFloor opposite() {
    return isCap() ? FLOOR : CAP;
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
