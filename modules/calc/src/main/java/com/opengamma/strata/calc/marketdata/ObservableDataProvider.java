/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.ObservableId;

/**
 * A provider of observable market data.
 * <p>
 * This plugin point allows a market data supplier to be provided.
 * Implementations might request data from an external data provider, such as Bloomberg or Reuters.
 */
public interface ObservableDataProvider {

  /**
   * Obtains an instance that provides no market data.
   * <p>
   * When invoked, the provider will return a map where every requested identifier is a failure.
   *
   * @return a provider that returns failures if invoked
   */
  public static ObservableDataProvider none() {
    return identifiers -> identifiers.stream()
        .collect(toImmutableMap(
            id -> id,
            id -> Result.failure(
                FailureReason.MISSING_DATA,
                "No observable market data provider configured, unable to provide data for '{}'",
                id)));
  }

  //-------------------------------------------------------------------------
  /**
   * Provides market data for the specified identifiers.
   * <p>
   * The implementation will provide market data for each identifier.
   * If market data cannot be obtained for an identifier, a failure will be returned.
   * The returned map must contain one entry for each identifier that was requested.
   *
   * @param identifiers  the market data identifiers to find
   * @return the map of market data values, keyed by identifier
   */
  public abstract Map<ObservableId, Result<Double>> provideObservableData(Set<? extends ObservableId> identifiers);

}
