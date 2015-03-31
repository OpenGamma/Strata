/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.marketdata.id.MarketDataId;

/**
 * Abstract market data builder that is useful as a base class for market data that is built one item at a time.
 * <p>
 * {@link MarketDataBuilder} implementations build multiple items of data in one call to the {@code build()}
 * method. This is to allow for more efficient implementations where data can be built in bulk.
 * <p>
 * However, many types of market data can't be built in bulk, and each implementation would have to implement
 * the same logic to loop over the requirements, build the data one item at a time and collect the results
 * into a map.
 * <p>
 * This class contains the looping and map building logic, and subtypes only need to implement the logic
 * to build the data.
 */
public abstract class SingleValueMarketDataBuilder<T, I extends MarketDataId<T>>
    implements MarketDataBuilder<T, I> {

  /** The type of {@link MarketDataId} handled by this builder. */
  private final Class<I> idType;

  /**
   * @param idType  the type of {@link MarketDataId} handled by this builder
   */
  protected SingleValueMarketDataBuilder(Class<I> idType) {
    this.idType = ArgChecker.notNull(idType, "idType");
  }

  @Override
  public abstract MarketDataRequirements requirements(I id);

  @Override
  public Map<I, Result<T>> build(Set<I> requirements, BaseMarketData builtData) {
    return requirements.stream().collect(toImmutableMap(id -> id, id -> buildSingleValue(id, builtData)));
  }

  /**
   * Builds a single item of market data.
   *
   * @param requirement  the ID of the market data value
   * @param builtData  market data containing any data required to build the market data value
   * @return a result containing the market data value or details of why it wasn't built
   */
  protected abstract Result<T> buildSingleValue(I requirement, BaseMarketData builtData);

  @Override
  public Class<I> getMarketDataIdType() {
    return idType;
  }
}
