/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMapping;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Builder for {@link MarketDataMappings} that knows about the standard mappings (e.g. curve to curve group)
 * and also allows arbitrary mappings to be added.
 */
public class MarketDataMappingsBuilder {

  /**
   * Market data feed that is the source for observable market data, for example Bloomberg or Reuters.
   */
  private final MarketDataFeed marketDataFeed;
  /**
   * Mappings that translate data requests from calculators into requests that can be used to look
   * up the data in the global set of market data.
   */
  private List<MarketDataMapping<?, ?>> mappings = new ArrayList<>();

  /**
   * Creates an instance.
   * 
   * @param marketDataFeed  the feed
   */
  private MarketDataMappingsBuilder(MarketDataFeed marketDataFeed) {
    this.marketDataFeed = marketDataFeed;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an empty builder with a market data feed of {@link MarketDataFeed#NONE}.
   *
   * @return an empty builder
   */
  public static MarketDataMappingsBuilder create() {
    return new MarketDataMappingsBuilder(MarketDataFeed.NONE);
  }

  /**
   * Returns an empty builder with the specified market data feed.
   *
   * @param marketDataFeed  the market data feed used in the mappings
   * @return an empty builder
   */
  public static MarketDataMappingsBuilder create(MarketDataFeed marketDataFeed) {
    return new MarketDataMappingsBuilder(marketDataFeed);
  }

  /**
   * Adds a mapping that sets the curve group used to look up curves.
   *
   * @param curveGroupName  the curve group used as the source for curves
   * @return this builder
   */
  public MarketDataMappingsBuilder curveGroup(CurveGroupName curveGroupName) {
    ArgChecker.notNull(curveGroupName, "curveGroupName");
    mappings.add(DiscountCurveMapping.of(curveGroupName, marketDataFeed));
    mappings.add(RateIndexCurveMapping.of(curveGroupName, marketDataFeed));
    mappings.add(DiscountFactorsMapping.of(curveGroupName, marketDataFeed));
    mappings.add(IborIndexRatesMapping.of(curveGroupName, marketDataFeed));
    mappings.add(OvernightIndexRatesMapping.of(curveGroupName, marketDataFeed));
    return this;
  }

  /**
   * Adds a an arbitrary mapping to the builder.
   *
   * @param mapping  a mapping that defines how market data requested by a calculation should be looked up
   *   in the global set of market data
   * @return this builder
   */
  public MarketDataMappingsBuilder mapping(MarketDataMapping<?, ?> mapping) {
    ArgChecker.notNull(mapping, "mapping");
    mappings.add(mapping);
    return this;
  }

  /**
   * Returns a set of market data mappings built from the data in this builder.
   *
   * @return a set of market data mappings built from the data in this builder
   */
  public MarketDataMappings build() {
    return MarketDataMappings.of(marketDataFeed, mappings);
  }

}
