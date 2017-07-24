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
 * The action to perform when the dates of two curve nodes clash.
 * <p>
 * See {@link CurveNodeDateOrder} for more details.
 */
public enum CurveNodeClashAction implements NamedEnum {

  /**
   * When a clash occurs, an exception is thrown.
   */
  EXCEPTION,
  /**
   * When a clash occurs, this node is dropped.
   */
  DROP_THIS,
  /**
   * When a clash occurs, the other node is dropped.
   */
  DROP_OTHER;

  // helper for name conversions
  private static final EnumNames<CurveNodeClashAction> NAMES = EnumNames.of(CurveNodeClashAction.class);

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
  public static CurveNodeClashAction of(String name) {
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
