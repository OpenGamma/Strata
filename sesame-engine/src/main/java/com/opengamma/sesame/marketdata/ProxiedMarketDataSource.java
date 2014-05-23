/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * A proxied market data sources that allow captured all the requests
 * and responses from its underlying source.
 */
public class ProxiedMarketDataSource implements MarketDataSource {

  private final MarketDataSource _underlying;

  private final Map<Pair<ExternalIdBundle, FieldName>, Result<?>> _marketDataRequests = new HashMap<>();

  /**
   * Create the proxied market data source, wrapping the provided source.
   *
   * @param marketDataSource the source to be proxied
   */
  public ProxiedMarketDataSource(MarketDataSource marketDataSource) {
    _underlying = marketDataSource;
  }

  /**
   * Executes the get request against the underlying source recording
   * the requests made and results returned.
   *
   * @param id  the external identifier of the data
   * @param fieldName  the name of the field in the market data record
   * @return a result indicating the market data value or the reason
   * the request could not be completed.
   */
  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    Result<?> result = _underlying.get(id, fieldName);
    _marketDataRequests.put(Pairs.of(id, fieldName), result);
    return result;
  }

  public Map<Pair<ExternalIdBundle, FieldName>, Result<?>> retrieveResults() {
    return ImmutableMap.copyOf(_marketDataRequests);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProxiedMarketDataSource that = (ProxiedMarketDataSource) o;
    return _underlying.equals(that._underlying);
  }

  @Override
  public int hashCode() {
    return _underlying.hashCode();
  }
}
