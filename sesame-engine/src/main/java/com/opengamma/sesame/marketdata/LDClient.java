package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fudgemsg.FudgeMsg;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

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
   * from the live data manager.
   */
  private boolean _valuesPending;

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
    _valuesPending = true;
  }

  /**
   * Subscribe to market data.
   *
   * @param requests the data to be subscribed to
   */
  public void subscribe(Set<Pair<ExternalIdBundle, FieldName>> requests) {

    if (!requests.isEmpty()) {
      Set<ExternalIdBundle> subscriptions = new HashSet<>();
      for (Pair<ExternalIdBundle, FieldName> request : requests) {

        ExternalIdBundle id = request.getFirst();

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
    if (_valuesPending) {
      _valuesPending = false;
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
