/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fudgemsg.FudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A source of live data that is primed with a set of market data
 * to be used within a single engine cycle. This class
 *
 *
 * REVIEW Chris 2014-03-05 - this doesn't look very thread safe to me
 */
public class ResettableLiveMarketDataSource implements StrategyAwareMarketDataSource {

  /** The logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(ResettableLiveMarketDataSource.class);
  
  /** The market data specification that this source satisfies. */
  private final MarketDataSpecification _marketDataSpecification;
  
  /** The live data client. */
  private final LDClient _liveDataClient;

  /** The market data. */
  private final Map<ExternalIdBundle, Result<FudgeMsg>> _data;

  /** Market data that has been requested. */
  private final Set<ExternalIdBundle> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<ExternalIdBundle, Boolean>());

  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpec, LDClient liveDataClient) {
    this(marketDataSpec, liveDataClient, ImmutableMap.<ExternalIdBundle, Result<FudgeMsg>>of());
  }
  
  private ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpecification, LDClient liveDataClient,
                                         Map<ExternalIdBundle, Result<FudgeMsg>> data) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
    _liveDataClient = ArgumentChecker.notNull(liveDataClient, "liveDataClient");
    _data = ArgumentChecker.notNull(data, "data");
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {

    Result<FudgeMsg> value = _data.get(id);

    if (value != null) {
      if (value.isSuccess()) {
        Object result = value.getValue().getValue(fieldName.getName());
        if (result != null) {
          return Result.success(result);
        } else {
          return Result.failure(
              FailureStatus.MISSING_DATA, "Data is available for id: {}, but not for field: {}", id, fieldName);
        }
      } else {
        return value;
      }
    } else {
      _requests.add(id);
      return Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
    }
  }

  public Set<ExternalIdBundle> getRequestedData() {
    return Collections.unmodifiableSet(_requests);
  }

  @Override
  public StrategyAwareMarketDataSource createPrimedSource() {
    _liveDataClient.subscribe(_requests);
    _liveDataClient.waitForSubscriptions();

    Map<ExternalIdBundle, Result<FudgeMsg>> data = new HashMap<>();

    // Iterate over the combination of newly requested data (to get initial
    // values for the request) and the existing data (to pick up any
    // updated values)
    for (ExternalIdBundle idBundle : Iterables.concat(_requests, _data.keySet())) {
      data.put(idBundle, generateRequestResult(idBundle));
    }

    return new ResettableLiveMarketDataSource(_marketDataSpecification, _liveDataClient, data);
  }

  private Result<FudgeMsg> generateRequestResult(ExternalIdBundle idBundle) {

    Map<ExternalIdBundle, Result<FudgeMsg>> latestData = _liveDataClient.retrieveLatestData();

    if (latestData.containsKey(idBundle)) {
      return latestData.get(idBundle);
    } else {
      // Fallback case that we really shouldn't see as in requesting
      // market data we should have at least generated a pending status
      s_logger.error("Mismatching market data subscriptions - no data received for id: {}", idBundle);
      return Result.failure(FailureStatus.MISSING_DATA, "No data available for id: {}", idBundle);
    }
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
