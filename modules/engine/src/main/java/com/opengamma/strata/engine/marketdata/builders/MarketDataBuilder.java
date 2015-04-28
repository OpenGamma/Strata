/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.marketdata.id.MarketDataId;

/**
 * A market data builder creates items of market data for a set of market data IDs.
 * <p>
 * A builder implementation produces a single type of market data and consumes a single type of market data ID.
 *
 * @param <T>  the type of the market data built by this class
 * @param <I>  the type of the market data ID handled by this class
 */
public interface MarketDataBuilder<T, I extends MarketDataId<? extends T>> {

  /**
   * Returns requirements representing the data needed to build the item of market data identified by the ID.
   *
   * @param id  an ID identifying an item of market data
   * @return requirements representing the data needed to build the item of market data identified by the ID
   */
  public abstract MarketDataRequirements requirements(I id);

  /**
   * Builds and returns the market data identified by the ID.
   * <p>
   * If the data cannot be built the result contains details of the problem.
   *
   * @param requirement  ID of the market data that should be built
   * @param builtData  a set of market data including any data required to build the requested data
   * @return built market data, or details of the problems that prevented building
   */
  public abstract Result<T> build(I requirement, BaseMarketData builtData);

  /**
   * @return the type of market data ID this builder can handle
   */
  public abstract Class<I> getMarketDataIdType();
}
