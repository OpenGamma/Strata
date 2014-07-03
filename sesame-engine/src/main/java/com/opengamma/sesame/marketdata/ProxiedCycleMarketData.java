/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * A proxied market data factory that allow capturing of all the requests
 * and responses from the underlying sources.
 */
public class ProxiedCycleMarketData implements CycleMarketDataFactory {

  private final CycleMarketDataFactory _underlying;

  private final ConcurrentMap<ZonedDateTime, ProxiedMarketDataSource> _marketDataProxies = new ConcurrentHashMap<>();

  /**
   * Create the proxied market data source, wrapping the provided source.
   *
   * @param cycleMarketDataFactory the source to be proxied
   */
  public ProxiedCycleMarketData(CycleMarketDataFactory cycleMarketDataFactory) {
    _underlying = cycleMarketDataFactory;
  }

  /**
   * Retrieves the results collected from the market data calls.
   *
   * @return the results collected from the market data calls,
   * keyed by the date the data was requested for
   */
  public Map<ZonedDateTime, Map<Pair<ExternalIdBundle, FieldName>, Result<?>>> retrieveMarketDataResults() {
    return Maps.transformValues(_marketDataProxies,
        new Function<ProxiedMarketDataSource, Map<Pair<ExternalIdBundle, FieldName>, Result<?>>>() {
          @Override
          public Map<Pair<ExternalIdBundle, FieldName>, Result<?>> apply(ProxiedMarketDataSource input) {
            return input.retrieveResults();
          }
        });
  }

  @Override
  public MarketDataSource getPrimaryMarketDataSource() {
    MarketDataSource marketDataSource = _underlying.getPrimaryMarketDataSource();
    // This allows us to store requests for the primary
    // source in the same map as historic sources but
    // it's not a great solution
    ZonedDateTime time = LocalDate.MAX.atStartOfDay(ZoneOffset.UTC);
    _marketDataProxies.putIfAbsent(time, new ProxiedMarketDataSource(marketDataSource));
    return _marketDataProxies.get(time);
  }

  @Override
  public MarketDataSource getMarketDataSourceForDate(ZonedDateTime valuationTime) {
    MarketDataSource marketDataSource = _underlying.getMarketDataSourceForDate(valuationTime);
    _marketDataProxies.putIfAbsent(valuationTime, new ProxiedMarketDataSource(marketDataSource));
    return _marketDataProxies.get(valuationTime);
  }

  @Override
  public CycleMarketDataFactory withMarketDataSpecification(MarketDataSpecification marketDataSpec) {
    // This method is only called when setting up a cycle, but the proxy
    // is only ever used within a cycle. Therefore we don't need to
    // actually do anything.
    throw new UnsupportedOperationException();
  }

  @Override
  public CycleMarketDataFactory withPrimedMarketDataSource() {
    // This method is only called when setting up a cycle, but the proxy
    // is only ever used within a cycle. Therefore we don't need to
    // actually do anything.
    throw new UnsupportedOperationException();
  }
}
