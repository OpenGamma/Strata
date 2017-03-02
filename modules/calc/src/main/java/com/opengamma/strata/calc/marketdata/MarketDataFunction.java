/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A market data function creates items of market data for a set of market data IDs.
 * <p>
 * A function implementation produces a single type of market data and consumes a single type of market data ID.
 *
 * @param <T>  the type of the market data built by this class
 * @param <I>  the type of the market data ID handled by this class
 */
public interface MarketDataFunction<T, I extends MarketDataId<? extends T>> {

  /**
   * Returns requirements representing the data needed to build the item of market data identified by the ID.
   *
   * @param id  an ID identifying an item of market data
   * @param marketDataConfig  configuration specifying how market data values should be built
   * @return requirements representing the data needed to build the item of market data identified by the ID
   */
  public abstract MarketDataRequirements requirements(I id, MarketDataConfig marketDataConfig);

  /**
   * Builds and returns the market data identified by the ID.
   * <p>
   * If the data cannot be built the result contains details of the problem.
   *
   * @param id  ID of the market data that should be built
   * @param marketDataConfig  configuration specifying how the market data should be built
   * @param marketData  a set of market data including any data required to build the requested data
   * @param refData  the reference data
   * @return built market data, or details of the problems that prevented building
   */
  public abstract MarketDataBox<T> build(
      I id,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData,
      ReferenceData refData);

  /**
   * Returns the type of market data ID this function can handle.
   *
   * @return the type of market data ID this function can handle
   */
  public abstract Class<I> getMarketDataIdType();
}
