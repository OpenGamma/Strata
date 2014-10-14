/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.exchange;

import java.time.ZoneId;

import com.opengamma.basics.location.Country;

/**
 * Standard exchage implementations.
 * <p>
 * See {@link Exchanges} for the description of each.
 */
final class StandardExchanges {

  // London Stock Exchange
  public static final ImmutableExchange XLON = ImmutableExchange.builder()
      .name("XLON")
      .country(Country.GB)
      .timeZone(ZoneId.of("Europe/London"))
      .build();

  // New York Stock Exchange
  public static final ImmutableExchange XNYS = ImmutableExchange.builder()
      .name("XNYS")
      .country(Country.US)
      .timeZone(ZoneId.of("America/New_York"))
      .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardExchanges() {
  }

}
