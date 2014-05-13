/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * A source of live data that is primed with a set of market data
 * to be used within a single engine cycle.
 */
public class ResettableLiveMarketDataSource implements StrategyAwareMarketDataSource {

  /** The market data specification that this source satisfies. */
  private final MarketDataSpecification _marketDataSpecification;
  
  /** The live data client. */
  private final LDClient _liveDataClient;

  /** The market data. */
  private final ImmutableLiveDataResultMapper _data;

  /** Market data that has been requested. */
  private final Set<ExternalIdBundle> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<ExternalIdBundle, Boolean>());

  /**
   * Create a ResettableLiveMarketDataSource for the specified market
   * data spec and live data client. Instance will be initialized
   * with an empty LiveDataResultMapper.
   *
   * @param marketDataSpecification  the specification for the market data
   * being retrieved, not null
   * @param liveDataClient  the client to use to retrieve the market
   * data, not null
   */
  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpecification, LDClient liveDataClient) {
    this(marketDataSpecification, liveDataClient, DefaultImmutableLiveDataResultMapper.EMPTY);
  }

  /**
   * Create a ResettableLiveMarketDataSource for the specified market
   * data spec and live data client, initialized with the supplied
   * LiveDataResultMapper.
   *
   * @param marketDataSpecification  the specification for the market data
   * being retrieved, not null
   * @param liveDataClient  the client to use to retrieve the market
   * data, not null
   * @param liveDataResultMapper  the result data to be used
   */
  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpecification, LDClient liveDataClient,
                                        ImmutableLiveDataResultMapper liveDataResultMapper) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
    _liveDataClient = ArgumentChecker.notNull(liveDataClient, "liveDataClient");
    _data = ArgumentChecker.notNull(liveDataResultMapper, "data");
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {

    if (!_data.containsKey(id)) {
      _requests.add(id);
    }

    LiveDataResult value = _data.containsKey(id) ? _data.get(id) : new PendingLiveDataResult(id);
    return value.getValue(fieldName);
  }

  public Set<ExternalIdBundle> getRequestedData() {
    return Collections.unmodifiableSet(_requests);
  }

  @Override
  public StrategyAwareMarketDataSource createPrimedSource() {
    _liveDataClient.subscribe(_requests);
    _liveDataClient.waitForSubscriptions();
    return new ResettableLiveMarketDataSource(
        _marketDataSpecification, _liveDataClient, _liveDataClient.retrieveLatestData());
  }

  @Override
  public void dispose() {
    _liveDataClient.dispose();
  }

  @Override
  public boolean isCompatible(MarketDataSpecification specification) {
    return _marketDataSpecification.equals(specification);
  }

}
