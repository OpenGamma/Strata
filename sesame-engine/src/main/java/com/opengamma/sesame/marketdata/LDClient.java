package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fudgemsg.FudgeMsg;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.LiveDataManager.SubscriptionRequest;
import com.opengamma.util.tuple.Pair;

// todo - this probably deserves to be a standalone class
// Note name is because LiveDataClient is already used elsewhere in OG
public class LDClient implements LiveDataManager.LDListener {

  private final LiveDataManager _liveDataManager;

  private final Map<ExternalIdBundle, FudgeMsg> _latestSnapshot = new HashMap<>();

  private final Set<ExternalIdBundle> _pendingSubscriptions = new HashSet<>();

  private final Map<ExternalIdBundle, String> _failedSubscriptions = new HashMap<>();

  private boolean _valuesPending;

  public LDClient(LiveDataManager liveDataManager) {
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
  
  public void dispose() {
    _liveDataManager.unregister(this);
  }

}