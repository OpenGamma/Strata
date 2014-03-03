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

public class ResettableLiveRawMarketDataSource implements RawMarketDataSource, LiveDataManager.LDListener {

  private final LiveDataManager _liveDataManager;

  private final Map<ExternalIdBundle, FudgeMsg> _latestSnapshot = new HashMap<>();

  private final Map<ExternalIdBundle, String> _failedSubscriptions = new HashMap<>();

  private boolean _valuesPending;

  public ResettableLiveRawMarketDataSource(LiveDataManager liveDataManager) {
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
  public MarketDataItem get(ExternalIdBundle idBundle, String dataField) {

    if (_latestSnapshot.containsKey(idBundle)) {
      final Object value = _latestSnapshot.get(idBundle).getValue(dataField);
      return value != null ?
          MarketDataItem.available(value) :
          MarketDataItem.UNAVAILBLE;
    } else if (_failedSubscriptions.containsKey(idBundle)) {
      return MarketDataItem.UNAVAILBLE;
    } else {
      _liveDataManager.makeSubscriptionRequest(
          new LiveDataManager.SubscriptionRequest<>(this, LiveDataManager.RequestType.SUBSCRIBE, idBundle));
      return MarketDataItem.PENDING;
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
