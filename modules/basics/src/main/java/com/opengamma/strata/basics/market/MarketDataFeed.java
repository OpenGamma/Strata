/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a feed of market data, for example Bloomberg or Reuters.
 * <p>
 * A feed can represent the default source of data for a particular data provider, or it can
 * represent a subset of the data from the provider, for example data from a specific broker
 * published by Bloomberg. Therefore there can be multiple feeds providing data from a single
 * physical market data system.
 */
public final class MarketDataFeed
    extends TypedString<MarketDataFeed> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /**
   * A market data feed used where a feed is required but no data is expected to be requested.
   */
  public static final MarketDataFeed NONE = of("None");

  /**
   * A market data feed used to indicate there are no market data rules for a calculation.
   */
  public static final MarketDataFeed NO_RULE = of("NoMatchingMarketDataRule");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Feed names must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the feed
   * @return a feed with the specified name
   */
  @FromString
  public static MarketDataFeed of(String name) {
    return new MarketDataFeed(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the feed
   */
  private MarketDataFeed(String name) {
    super(name, NAME_PATTERN, "Feed name must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
