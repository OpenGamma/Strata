/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * A StrategyAwareMarketDataSource suitable for wrapping eager data
 * sources. As such it does not need to capture information about
 * requests made, it just serves the data directly from the
 * underlying market data source.
 */
public class DefaultStrategyAwareMarketDataSource implements StrategyAwareMarketDataSource {

  /**
   * The underlying eager market data source.
   */
  private final MarketDataSource _marketDataSource;

  /**
   * Create the market data source wrapping the eager underlying
   * market data source.
   *
   * @param marketDataSource the eager market data source to be wrapped, not null
   */
  public DefaultStrategyAwareMarketDataSource(MarketDataSource marketDataSource) {
    _marketDataSource = ArgumentChecker.notNull(marketDataSource, "marketDataSource");
  }

  /**
   * Attempts to get the requested market data item from the underlying source.
   *
   * @param id the ID of the data
   * @param fieldName the name of the field in the market data record
   * @return the market data if found
   */
  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    return _marketDataSource.get(id, fieldName);
  }

  /**
   * Returns an immutable empty set, as this is wrapping an eager data source.
   *
   * @return an immutable empty set
   */
  @Override
  public Set<Pair<ExternalIdBundle, FieldName>> getRequestedData() {
    return ImmutableSet.of();
  }


  /**
   * Returns an immutable empty set, as this is wrapping an eager data source.
   *
   * @return an immutable empty set
   */
  @Override
  public Set<Pair<ExternalIdBundle, FieldName>> getManagedData() {
    return ImmutableSet.of();
  }

  /**
   * Returns true as this is wrapping an eager data source.
   *
   * @return true
   */
  @Override
  public boolean isEagerDataSource() {
    return true;
  }
}
