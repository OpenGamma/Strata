/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.function;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.Result;

// TODO Does this need to take some kind of data source? Or will they be given to it on construction?
/**
 * A function for building for items of observable market data.
 * <p>
 * Implementations will typically request data from an external data provider, for example Bloomberg or Reuters.
 */
public interface ObservableMarketDataFunction {

  /**
   * Returns a builder that doesn't build any market data.
   *
   * @return a builder that doesn't build any market data
   */
  public static ObservableMarketDataFunction none() {
    return requirements -> ImmutableMap.of();
  }

  /**
   * Returns market data values for the IDs in {@code requirements} or the details of why the data
   * couldn't be built.
   *
   * @param requirements  the market data that should be built
   * @return the market data values or details of the reason the values couldn't be built
   */
  public abstract Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements);
}
