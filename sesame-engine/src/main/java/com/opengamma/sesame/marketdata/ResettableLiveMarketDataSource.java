/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;

import org.fudgemsg.FudgeMsg;

import com.opengamma.id.ExternalIdBundle;

/**
 * REVIEW Chris 2014-03-05 - this doesn't look very thread safe to me
 */
public class ResettableLiveMarketDataSource implements MarketDataSource, LiveDataManager.LDListener {

  private final LiveDataManager _liveDataManager;

  private final Map<ExternalIdBundle, FudgeMsg> _latestSnapshot = new HashMap<>();

  private final Map<ExternalIdBundle, String> _failedSubscriptions = new HashMap<>();

  private boolean _valuesPending;

  public ResettableLiveMarketDataSource(LiveDataManager liveDataManager) {
    _liveDataManager = liveDataManager;
  }

  public void reset() {
    if (_valuesPending) {
      _valuesPending = false;
      _latestSnapshot.clear();
      _latestSnapshot.putAll(_liveDataManager.snapshot(this));
    }
  }

  public void waitForData() {
    _liveDataManager.waitForAllData(this);
    reset();
  }

  @Override
  public MarketDataItem get(ExternalIdBundle idBundle, FieldName fieldName) {

    if (_latestSnapshot.containsKey(idBundle)) {
      final Object value = _latestSnapshot.get(idBundle).getValue(fieldName.getName());
      return value != null ? MarketDataItem.available(value) : MarketDataItem.unavailable();
    } else if (_failedSubscriptions.containsKey(idBundle)) {
      return MarketDataItem.unavailable();
    } else {
      _liveDataManager.makeSubscriptionRequest(
          new LiveDataManager.SubscriptionRequest<>(this, LiveDataManager.RequestType.SUBSCRIBE, idBundle));
      return MarketDataItem.pending();
    }
  }

  @Override
  public void receiveSubscriptionResponse(LiveDataManager.SubscriptionResponse<ExternalIdBundle> subscriptionResponse) {
    _failedSubscriptions.putAll(subscriptionResponse.getFailures());
  }

  @Override
  public void valueUpdated(ExternalIdBundle idBundle) {
    _valuesPending = true;
  }
}
