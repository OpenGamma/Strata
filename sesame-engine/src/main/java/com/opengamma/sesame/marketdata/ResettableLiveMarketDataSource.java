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
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

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
  private final Map<Pair<ExternalIdBundle, FieldName>, Result<?>> _data;

  /** Market data that has been requested. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _requests =
      Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>());

  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpec, LDClient liveDataClient) {
    this(marketDataSpec, liveDataClient, ImmutableMap.<Pair<ExternalIdBundle, FieldName>, Result<?>>of());
  }
  
  private ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpecification, LDClient liveDataClient,
      Map<Pair<ExternalIdBundle, FieldName>, Result<?>> data) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
    _liveDataClient = ArgumentChecker.notNull(liveDataClient, "liveDataClient");
    _data = ArgumentChecker.notNull(data, "data");
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {

    Pair<ExternalIdBundle, FieldName> key = Pairs.of(id, fieldName);
    Result<?> value = _data.get(key);

    if (value != null) {
      return value;
    } else {
      _requests.add(key);
      return Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
    }
  }

  public Set<Pair<ExternalIdBundle, FieldName>> getRequestedData() {
    return Collections.unmodifiableSet(_requests);
  }

  @Override
  public StrategyAwareMarketDataSource createPrimedSource() {
    Set<Pair<ExternalIdBundle, FieldName>> requests = getRequestedData();
    _liveDataClient.subscribe(requests);
    _liveDataClient.waitForSubscriptions();

    Map<ExternalIdBundle, Result<FudgeMsg>> latestData = _liveDataClient.retrieveLatestData();

    Map<Pair<ExternalIdBundle, FieldName>, Result<?>> data = new HashMap<>();

    // Iterate over the combination of newly requested data (to get initial
    // values for the request) and the existing data (to pick up any
    // updated values)
    for (Pair<ExternalIdBundle, FieldName> request : Iterables.concat(requests, _data.keySet())) {
      ExternalIdBundle idBundle = request.getFirst();
      FieldName fieldName = request.getSecond();
      if (latestData.containsKey(idBundle)) {
        Result<FudgeMsg> result = latestData.get(idBundle);
        if (result.isSuccess()) {
          FudgeMsg msg = result.getValue();
          Object value = msg.getValue(fieldName.getName());
          if (value != null) {
            data.put(request, Result.success(value));
          } else {
            data.put(request, Result.failure(FailureStatus.MISSING_DATA, "No data available for id: {}, field: {}", idBundle, fieldName));
          }
        } else {
          data.put(request, result);
        }
      } else {
        // Fallback case that we really shouldn't see as in requesting
        // market data we should have generated a pending status
        s_logger.error("Mismatching market data subscriptions - no tracking for request: {}/{}", idBundle, fieldName);
        data.put(request, Result.failure(FailureStatus.MISSING_DATA, "No data available for id: {}, field: {}", idBundle, fieldName));
      }
    }

    return new ResettableLiveMarketDataSource(_marketDataSpecification, _liveDataClient, data);
  }

  @Override
  public void dispose() {
    _liveDataClient.dispose();
  }

  @Override
  public boolean isCompatible(MarketDataSpecification specification) {
    return _marketDataSpecification.equals(specification);
  }
  
  /**
   * Builder for the class.
   */
  public static final class Builder {

    private final MarketDataSpecification _marketDataSpecification;
    private final LDClient _liveDataClient;
    private final Map<Pair<ExternalIdBundle, FieldName>, Result<?>> _data = new HashMap<>();

    public Builder(MarketDataSpecification marketDataSpecification, LDClient liveDataClient) {
      _marketDataSpecification = marketDataSpecification;
      _liveDataClient = liveDataClient;
    }
    
    public Builder data(Map<Pair<ExternalIdBundle, FieldName>, Object> data) {
      ArgumentChecker.notNull(data, "data");
      for (Map.Entry<Pair<ExternalIdBundle, FieldName>, Object> entry : data.entrySet()) {
        _data.put(entry.getKey(), Result.success(entry.getValue()));
      }
      return this;
    }

    public Builder data(FieldName fieldName, Map<ExternalIdBundle, ?> data) {
      ArgumentChecker.notNull(fieldName, "fieldName");

      for (Map.Entry<ExternalIdBundle, ?> entry : data.entrySet()) {
        _data.put(Pairs.of(entry.getKey(), fieldName), Result.success(entry.getValue()));
      }
      return this;
    }

    public Builder data(ExternalIdBundle id, FieldName fieldName, Object data) {
      _data.put(key(id, fieldName), Result.success(ArgumentChecker.notNull(data, "data")));
      return this;
    }

    public Builder pending(Set<Pair<ExternalIdBundle, FieldName>> pending) {
      for (Pair<ExternalIdBundle, FieldName> pair : pending) {
        _data.put(pair, Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", pair.getFirst(), pair.getSecond()));
      }
      return this;
    }

    public Builder pending(ExternalIdBundle id, FieldName fieldName) {
      _data.put(key(id, fieldName), Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName));
      return this;
    }

    public Builder missing(Set<Pair<ExternalIdBundle, FieldName>> missing) {
      for (Pair<ExternalIdBundle, FieldName> pair : missing) {
        _data.put(pair, Result.failure(FailureStatus.MISSING_DATA, "No data available for: {}", pair.getFirst()));
      }
      return this;
    }

    public Builder missing(ExternalIdBundle id, FieldName fieldName) {
      _data.put(key(id, fieldName), Result.failure(FailureStatus.MISSING_DATA, "No data available for: {}", id));
      return this;
    }

    public ResettableLiveMarketDataSource build() {
      return new ResettableLiveMarketDataSource(_marketDataSpecification, _liveDataClient, _data);
    }

    private static Pair<ExternalIdBundle, FieldName> key(ExternalIdBundle id, FieldName fieldName) {
      return Pairs.of(ArgumentChecker.notNull(id, "id"), ArgumentChecker.notNull(fieldName, "fieldName"));
    }
  }
  
}
