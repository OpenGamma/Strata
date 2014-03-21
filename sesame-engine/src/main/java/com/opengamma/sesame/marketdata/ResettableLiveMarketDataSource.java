/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fudgemsg.FudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
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
  private final Map<Pair<ExternalIdBundle, FieldName>, Object> _data;

  /** Market data that has been requested. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _requests = Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>());

  /** Market data that was previously requested and is still pending. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _pending;

  /** Market data that has been requested and is known to be unavailable. */
  private final Set<Pair<ExternalIdBundle, FieldName>> _missing;

  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpec, LDClient liveDataClient) {
    this(marketDataSpec, liveDataClient, new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Object>(),
        Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>()),
        Collections.newSetFromMap(new ConcurrentHashMap<Pair<ExternalIdBundle, FieldName>, Boolean>()));
  }
  
  public ResettableLiveMarketDataSource(MarketDataSpecification marketDataSpecification, LDClient liveDataClient,
      Map<Pair<ExternalIdBundle, FieldName>, Object> data, Set<Pair<ExternalIdBundle, FieldName>> pending,
      Set<Pair<ExternalIdBundle, FieldName>> missing) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
    _liveDataClient = ArgumentChecker.notNull(liveDataClient, "liveDataClient");
    _data = ArgumentChecker.notNull(data, "data");
    _pending = ArgumentChecker.notNull(pending, "pending");
    _missing = ArgumentChecker.notNull(missing, "missing");
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    if (_missing.contains(Pairs.of(id, fieldName))) {
      return Result.failure(FailureStatus.MISSING_DATA, "No data available for {}/{}", id, fieldName);
    }
    // data is already pending, no need to add it to requests and ask for it again
    if (_pending.contains(Pairs.of(id, fieldName))) {
      return Result.failure(FailureStatus.PENDING_DATA, "Already requested data for {}/{}", id, fieldName);
    }
    Pair<ExternalIdBundle, FieldName> key = Pairs.of(id, fieldName);
    Object value = _data.get(key);

    if (value != null) {
      return Result.success(value);
    } else {
      _requests.add(key);
      return Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
    }
  }

  @Override
  public Set<Pair<ExternalIdBundle, FieldName>> getRequestedData() {
    return Collections.unmodifiableSet(_requests);
  }

  @Override
  public Set<Pair<ExternalIdBundle, FieldName>> getManagedData() {
    return Collections.unmodifiableSet(_data.keySet());
  }

  @Override
  public StrategyAwareMarketDataSource createPrimedSource() {
    Set<Pair<ExternalIdBundle, FieldName>> requests = getRequestedData();
    _liveDataClient.subscribe(requests);
    _liveDataClient.waitForSubscriptions();

    Map<ExternalIdBundle, FudgeMsg> latestData = _liveDataClient.retrieveLatestData();
    Set<ExternalIdBundle> latestPending = _liveDataClient.getPending();
    Map<ExternalIdBundle, String> latestFailures = _liveDataClient.getFailures();

    Map<Pair<ExternalIdBundle, FieldName>, Object> data = new HashMap<>();
    Set<Pair<ExternalIdBundle, FieldName>> pending = new HashSet<>();
    Set<Pair<ExternalIdBundle, FieldName>> missing = new HashSet<>();

    for (Pair<ExternalIdBundle, FieldName> request : Iterables.concat(requests, getManagedData())) {
      ExternalIdBundle idBundle = request.getFirst();
      FieldName fieldName = request.getSecond();
      if (latestData.containsKey(idBundle)) {
        Object value = latestData.get(idBundle).getValue(fieldName.getName());
        if (value != null) {
          data.put(request, value);
        } else {
          s_logger.warn("No live market data found for {}/{}", idBundle, fieldName);
          missing.add(request);
        }
      } else if (latestPending.contains(idBundle)) {
        pending.add(request);
      } else if (latestFailures.containsKey(idBundle)) {
        missing.add(request);
      } else {
        // Fallback case that we really shouldn't see
        s_logger.error("Mismatching market data subscriptions - no tracking for request: {}/{}", idBundle, fieldName);
      }
    }

    if (!pending.isEmpty()) {
      // todo - seeing this indicates the latches for awaiting market data are not yet correct
      s_logger.warn("Waited for market data subscriptions but still have pending data: {}", pending);
    }

    return new ResettableLiveMarketDataSource(_marketDataSpecification, _liveDataClient, data, pending, missing);
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
   * 
   */
  public static final class Builder {

    private final MarketDataSpecification _marketDataSpecification;
    private final LDClient _liveDataClient;
    private final Map<Pair<ExternalIdBundle, FieldName>, Object> _data = new HashMap<>();
    private final Set<Pair<ExternalIdBundle, FieldName>> _pending = new HashSet<>();
    private final Set<Pair<ExternalIdBundle, FieldName>> _missing = new HashSet<>();

    public Builder(MarketDataSpecification marketDataSpecification, LDClient liveDataClient) {
      _marketDataSpecification = marketDataSpecification;
      _liveDataClient = liveDataClient;
    }
    
    public Builder data(Map<Pair<ExternalIdBundle, FieldName>, Object> data) {
      _data.putAll(ArgumentChecker.notNull(data, "data"));
      return this;
    }

    public Builder data(FieldName fieldName, Map<ExternalIdBundle, ?> data) {
      ArgumentChecker.notNull(fieldName, "fieldName");

      for (Map.Entry<ExternalIdBundle, ?> entry : data.entrySet()) {
        _data.put(Pairs.of(entry.getKey(), fieldName), entry.getValue());
      }
      return this;
    }

    public Builder data(ExternalIdBundle id, FieldName fieldName, Object data) {
      _data.put(key(id, fieldName), ArgumentChecker.notNull(data, "data"));
      return this;
    }

    public Builder pending(Set<Pair<ExternalIdBundle, FieldName>> pending) {
      _pending.addAll(ArgumentChecker.notNull(pending, "pending"));
      return this;
    }

    public Builder pending(ExternalIdBundle id, FieldName fieldName) {
      _pending.add(key(id, fieldName));
      return this;
    }

    public Builder missing(Set<Pair<ExternalIdBundle, FieldName>> missing) {
      _missing.addAll(ArgumentChecker.notNull(missing, "missing"));
      return this;
    }

    public Builder missing(ExternalIdBundle id, FieldName fieldName) {
      _missing.add(key(id, fieldName));
      return this;
    }

    public ResettableLiveMarketDataSource build() {
      return new ResettableLiveMarketDataSource(_marketDataSpecification, _liveDataClient, _data, _pending, _missing);
    }

    private static Pair<ExternalIdBundle, FieldName> key(ExternalIdBundle id, FieldName fieldName) {
      return Pairs.of(ArgumentChecker.notNull(id, "id"), ArgumentChecker.notNull(fieldName, "fieldName"));
    }
  }
  
}
