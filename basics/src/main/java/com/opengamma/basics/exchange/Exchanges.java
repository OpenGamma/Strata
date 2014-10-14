/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.exchange;

import com.opengamma.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard exchanges.
 * <p>
 * Each constant returns a standard definition of the specified exchange.
 */
public final class Exchanges {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<Exchange> ENUM_LOOKUP = ExtendedEnum.of(Exchange.class);

  /**
   * The London Stock Exchange.
   */
  public static final Exchange XLON = Exchange.of(StandardExchanges.XLON.getName());
  /**
   * The New York Stock Exchange.
   */
  public static final Exchange XNYS = Exchange.of(StandardExchanges.XNYS.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private Exchanges() {
  }

}
