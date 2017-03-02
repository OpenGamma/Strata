/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.NamedMarketDataId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;

/**
 * Encapsulates a rule or set of rules to decide whether a perturbation applies to a piece of market data.
 * <p>
 * A {@link ScenarioPerturbation} encapsulates a specific change to a piece of market data, such as a parallel shift.
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
   * Obtains a filter that matches any value with the specified identifier type.
   *
   * @param <T>  the type of market data handled by the filter
   * @param type  the type that is matched by this filter
   * @return a filter matching the specified type
   */
  public static <T> MarketDataFilter<T, MarketDataId<T>> ofIdType(Class<? extends MarketDataId<T>> type) {
    return new IdTypeFilter<T>(type);
  }

  /**
   * Obtains a filter that matches the specified identifier.
   *
   * @param <T>  the type of market data handled by the filter
   * @param id  the identifier that is matched by this filter
   * @return a filter matching the specified identifier
   */
  public static <T> MarketDataFilter<T, MarketDataId<T>> ofId(MarketDataId<T> id) {
    return new IdFilter<T>(id);
  }

  /**
   * Obtains a filter that matches the specified name.
   *
   * @param <T>  the type of market data handled by the filter
   * @param name  the name that is matched by this filter
   * @return a filter matching the specified name
   */
  public static <T> MarketDataFilter<T, NamedMarketDataId<T>> ofName(MarketDataName<T> name) {
    return new NameFilter<T>(name);
  }

  //-------------------------------------------------------------------------
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
   * @param refData  the reference data
   * @return true if the filter matches
   */
  public abstract boolean matches(I marketDataId, MarketDataBox<T> marketData, ReferenceData refData);

}
