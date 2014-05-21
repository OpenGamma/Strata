/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashSet;
import java.util.Set;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;

/**
 * A proxied market data sources that add methods that allow an
 * underlying source to have its requests and responses captured.
 */
public class ProxiedMarketDataSource implements MarketDataSource {

  private final MarketDataSource _underlying;

  private final Set<MarketDataSourceListener> _listeners = new HashSet<>();

  /**
   * Create the proxied market data source, wrapping the provided source.
   *
   * @param marketDataSource the source to be proxied
   */
  public ProxiedMarketDataSource(MarketDataSource marketDataSource) {
    _underlying = marketDataSource;
  }

  /**
   * Executes the get request against the underlying source but
   * informs any registered listeners of the request and the
   * result.
   *
   * @param id  the external identifier of the data
   * @param fieldName  the name of the field in the market data record
   * @return a result indicating the market data value or the reason
   * the request could not be completed.
   */
  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    Result<?> result = _underlying.get(id, fieldName);

    for (MarketDataSourceListener listener : _listeners) {
      listener.requestMade(id, fieldName, result);
    }
    return result;
  }

  /**
   * Adds a listener for requests to the source. The listener will
   * get informed of all requests made and responses returned.
   *
   * @param marketDataSourceListener the listener to be added
   */
  public void addListener(MarketDataSourceListener marketDataSourceListener) {
    _listeners.add(marketDataSourceListener);
  }

  /**
   * Removes a listener from the source.
   *
   * @param marketDataSourceListener the listener to be removed
   */
  public void removeListener(MarketDataSourceListener marketDataSourceListener) {
    _listeners.remove(marketDataSourceListener);
  }
}
