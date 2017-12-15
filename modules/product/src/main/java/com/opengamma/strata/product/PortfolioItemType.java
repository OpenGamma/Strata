/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The type of a portfolio item.
 * <p>
 * This allows trades and positions to be separated.
 */
public enum PortfolioItemType implements NamedEnum {

  /**
   * A trade.
   * <p>
   * A trade is a transaction that occurred on a specific date between two counterparties.
   * See {@link Trade}.
   */
  TRADE,
  /**
   * A position.
   * <p>
   * A position is effectively the sum of one or more trades in a {@link Security}.
   * See {@link Position}.
   */
  POSITION,
  /**
   * Neither a trade nor a position.
   */
  OTHER;

  // helper for name conversions
  private static final EnumNames<PortfolioItemType> NAMES = EnumNames.of(PortfolioItemType.class);

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
  public static PortfolioItemType of(String name) {
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
