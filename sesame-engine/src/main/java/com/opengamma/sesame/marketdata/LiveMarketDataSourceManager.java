/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.marketdata.LiveDataManager.SubscriptionRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fudgemsg.FudgeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 * Manages the live (non-eager) market data sources for a single
 * client of the engine. The {@link StrategyAwareMarketDataSource}
 * instances that this class creates can then be examined between
 * requests to determine the requests to be made from the live market
 * data server.
 */
public class LiveMarketDataSourceManager implements MarketDataSourceManager {

  /**
   * Logger for the class.
   */
  private static final Logger s_logger = LoggerFactory.getLogger(LiveMarketDataSourceManager.class);

  /**
   * The market data factory servicing non-live market data requests, not null.
   */
  private final MarketDataFactory _marketDataFactory;

  /**
   * The live data client servicing this market data factory, not null.
   */
  private final LDClient _liveDataClient;

  /**
   * Create the manager.
   *
   * @param marketDataFactory the market data factory servicing non-live
   * market data requests, not null
   * @param liveDataManager the live data client servicing this market data
   * factory, not null
   */
  public LiveMarketDataSourceManager(MarketDataFactory marketDataFactory, LiveDataManager liveDataManager) {
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFactory");
    _liveDataClient = new LDClient(ArgumentChecker.notNull(liveDataManager, "liveDataManager"));
  }

  @Override
  public StrategyAwareMarketDataSource createStrategyAwareSource(StrategyAwareMarketDataSource previousDataSource,
                                                                 MarketDataSpecification marketDataSpec) {

    return marketDataSpec instanceof LiveMarketDataSpecification ?
        createSourceForLiveData(previousDataSource) :
        // For eager data, the previous source doesn't hold
        // anything of interest to us
        createEagerSource(marketDataSpec);
  }

  private StrategyAwareMarketDataSource createSourceForLiveData(StrategyAwareMarketDataSource previousDataSource) {

    // If we had a non-eager source before then we know what data
    // is expected and can use it to prime the next source. Otherwise
    // we have to start from scratch.
    return previousDataSource.isEagerDataSource() ?
        new RecordingMarketDataSource() :
        waitForPrimedSource(previousDataSource);
  }

  private StrategyAwareMarketDataSource createEagerSource(MarketDataSpecification marketDataSpec) {
    return new DefaultStrategyAwareMarketDataSource(_marketDataFactory.create(marketDataSpec));
  }

  @Override
  public StrategyAwareMarketDataSource waitForPrimedSource(StrategyAwareMarketDataSource previousDataSource) {

    if (!previousDataSource.isEagerDataSource()) {
      Set<Pair<ExternalIdBundle, FieldName>> requests = previousDataSource.getRequestedData();
      _liveDataClient.subscribe(requests);
      _liveDataClient.waitForSubscriptions();

      Map<ExternalIdBundle, FudgeMsg> latestData = _liveDataClient.retrieveLatestData();
      Set<ExternalIdBundle> latestPending = _liveDataClient.getPending();
      Map<ExternalIdBundle, String> latestFailures = _liveDataClient.getFailures();

      Map<Pair<ExternalIdBundle, FieldName>, Object> data = new HashMap<>();
      Set<Pair<ExternalIdBundle, FieldName>> pending = new HashSet<>();
      Set<Pair<ExternalIdBundle, FieldName>> missing = new HashSet<>();

      for (Pair<ExternalIdBundle, FieldName> request : Iterables.concat(requests, previousDataSource.getManagedData())) {
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

      return new RecordingMarketDataSource(data, pending, missing);
    } else {
      return previousDataSource;
    }
  }

  // todo - this probably deserves to be a standalone class
  // Note name is because LiveDataClient is already used elsewhere in OG
  private class LDClient implements LiveDataManager.LDListener {

    private final LiveDataManager _liveDataManager;

    private final Map<ExternalIdBundle, FudgeMsg> _latestSnapshot = new HashMap<>();

    private final Set<ExternalIdBundle> _pendingSubscriptions = new HashSet<>();

    private final Map<ExternalIdBundle, String> _failedSubscriptions = new HashMap<>();

    private boolean _valuesPending;

    private LDClient(LiveDataManager liveDataManager) {
      _liveDataManager = liveDataManager;
    }

    @Override
    public void receiveSubscriptionResponse(LiveDataManager.SubscriptionResponse<ExternalIdBundle> subscriptionResponse) {
      _failedSubscriptions.putAll(subscriptionResponse.getFailures());
      _pendingSubscriptions.removeAll(subscriptionResponse.getSuccesses());
    }

    @Override
    public void valueUpdated(ExternalIdBundle idBundle) {
      _valuesPending = true;
    }

    public void subscribe(Set<Pair<ExternalIdBundle, FieldName>> requests) {

      if (!requests.isEmpty()) {
        Set<ExternalIdBundle> subscriptions = new HashSet<>();
        for (Pair<ExternalIdBundle, FieldName> request : requests) {

          ExternalIdBundle id = request.getFirst();

          if (!_latestSnapshot.containsKey(id) && !_failedSubscriptions.containsKey(id) && !_pendingSubscriptions.contains(id)) {
            subscriptions.add(id);
          }
        }
        _pendingSubscriptions.addAll(subscriptions);
        if (!subscriptions.isEmpty()) {
          _liveDataManager.makeSubscriptionRequest(createSubscriptionRequest(subscriptions));
        }
      }
    }

    private SubscriptionRequest<ExternalIdBundle> createSubscriptionRequest(Set<ExternalIdBundle> subscriptions) {
      return new SubscriptionRequest<>(this, LiveDataManager.RequestType.SUBSCRIBE, subscriptions);
    }

    public void waitForSubscriptions() {
      _liveDataManager.waitForAllData(this);
    }

    public Map<ExternalIdBundle, FudgeMsg> retrieveLatestData() {
      if (_valuesPending) {
        _valuesPending = false;
        _latestSnapshot.clear();
        _latestSnapshot.putAll(_liveDataManager.snapshot(this));
      }
      return ImmutableMap.copyOf(_latestSnapshot);
    }

    public Map<ExternalIdBundle, String> getFailures() {
      return ImmutableMap.copyOf(_failedSubscriptions);
    }

    public Set<ExternalIdBundle> getPending() {
      return _pendingSubscriptions;
    }

  }
}
