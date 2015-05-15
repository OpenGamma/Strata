/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenarios;

import com.opengamma.strata.basics.market.MarketDataId;

/**
 * Encapsulates a rule or set of rules to decide whether a {@link Perturbation} applies to a piece of market data.
 * <p>
 * For example, a filter could apply to all yield curves whose currency is USD, or quoted prices of equity securities
 * in the pharmaceutical sector.
 * <p>
 * Market data filter implementations should generally implement the Joda Beans {@code ImmutableBean} interface
 * which allows them to be serialized and used with a remote implementation of the engine API.
 *
 * @param <T>  the type of the market data handled by the filter
 * @param <I>  the type of the market data ID handled by the filter
 */
public interface MarketDataFilter<T, I extends MarketDataId<T>> {

  /**
   * Applies the filter to a market data ID and the corresponding market data value and returns true
   * if the filter matches.
   *
   * @param marketDataId  the ID of a piece of market data
   * @param marketData  the market data value
   * @return true if the filter matches
   */
  public abstract boolean apply(I marketDataId, T marketData);

  /**
   * Returns the type of market data ID handled by this filter.
   * <p>
   * This should correspond to the type parameter {@code I}.
   *
   * @return the type of market data ID handled by this filter
   */
  public abstract Class<?> getMarketDataIdType();
}
