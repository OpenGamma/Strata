/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.convert.FromString;

import com.opengamma.collect.type.TypedString;

/**
 * The type of a trade.
 * <p>
 * This identifies the type of a trade at the data model level.
 */
public final class TradeType
    extends TypedString<TradeType> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains a {@code TradeType} by name.
   * 
   * @param name  the name to lookup, not null
   * @return the type matching the name, not null
   */
  @FromString
  public static TradeType of(String name) {
    return new TradeType(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  private TradeType(String name) {
    super(name);
  }

}
