/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fudgemsg.FudgeMsg;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;

/**
 * Client which subscribes to market data via the LiveDataManager.
 * As {@link LiveDataManager} keeps track of which client has which
 * subscriptions the client needs to be used across multiple engine
 * cycles for a view.
 */
// Note name is because LiveDataClient is already used elsewhere in OG
public class LDClient implements LDListener {

  /**
   * The manager to get the live data from.
   */
  private final LiveDataManager _liveDataManager;

  /**
   * The latest set of values retrieved from the market data manager.
   */
  private final Map<ExternalIdBundle, Result<FudgeMsg>> _latestSnapshot = new HashMap<>();

  /**
   * Flag indicating whether updated values are available
   * from the live data manager. As it will be read and
   * written from multiple threads we must ensure we have
   * a consistent value, hence using the atomic reference.
   * <p>
   * Note that we initialise to true as if we don't and no
   * subscriptions need to be made (as another client has
   * requested the data already) and no data updates are
   * received from the provider, then we will create an
   * empty snapshot.
   */
  private final AtomicBoolean _valuesPending = new AtomicBoolean(true);

  /**
   * Create the client.
   *
   * @param liveDataManager the live data manager
   */
  public LDClient(LiveDataManager liveDataManager) {
    _liveDataManager = liveDataManager;
  }

  @Override
  public void valueUpdated() {
    _valuesPending.set(true);
  }

  /**
   * Subscribe to market data.
   *
   * @param requests the data to be subscribed to
   */
  public void subscribe(Set<ExternalIdBundle> requests) {

    if (!requests.isEmpty()) {
      Set<ExternalIdBundle> subscriptions = new HashSet<>();
      for (ExternalIdBundle id : requests) {
        if (!_latestSnapshot.containsKey(id)) {
          subscriptions.add(id);
        }
      }
      if (!subscriptions.isEmpty()) {
        _liveDataManager.subscribe(this, subscriptions);
      }
    }
  }

  // TODO - should handle unsubscription once the market data source needs it

  /**
   * Wait until results for all market data are available.
   */
  public void waitForSubscriptions() {
    _liveDataManager.waitForAllData(this);
  }

  /**
   * Get the latest market data if it has been updated since last
   * requested. If not updated the previous snapshot will be
   * returned.
   *
   * @return the latest market data
   */
  public Map<ExternalIdBundle, Result<FudgeMsg>> retrieveLatestData() {
    if (_valuesPending.compareAndSet(true, false)) {
      _latestSnapshot.clear();
      _latestSnapshot.putAll(_liveDataManager.snapshot(this));
    }
    return ImmutableMap.copyOf(_latestSnapshot);
  }

  /**
   * Remove all subscriptions for this client. To be called when
   * this client has completed all its work.
   */
  public void dispose() {
    _liveDataManager.unregister(this);
  }

}
