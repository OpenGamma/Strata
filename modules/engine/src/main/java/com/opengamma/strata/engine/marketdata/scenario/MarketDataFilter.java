/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenario;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.Perturbation;

/**
 * Encapsulates a rule or set of rules to decide whether a perturbation applies to a piece of market data.
 * <p>
 * A {@link Perturbation} encapsulates a specific change to a piece of market data, such as a parallel shift.
 * An implementation of this filter interface defines when the perturbation should be used.
 * For example, a filter could apply to all yield curves whose currency is USD, or quoted prices
 * of equity securities in the energy sector.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 *
 * @param <T>  the type of the market data handled by the filter
 * @param <I>  the type of the market data ID handled by the filter
 */
public interface MarketDataFilter<T, I extends MarketDataId<T>> {

  /**
   * Returns the type of market data ID handled by this filter.
   * <p>
   * This should correspond to the type parameter {@code I}.
   *
   * @return the type of market data ID handled by this filter
   */
  public abstract Class<?> getMarketDataIdType();

  /**
   * Applies the filter to a market data ID and the corresponding market data value
   * and returns true if the filter matches.
   *
   * @param marketDataId  the ID of a piece of market data
   * @param marketData  the market data value
   * @return true if the filter matches
   */
  public abstract boolean matches(I marketDataId, T marketData);

}
