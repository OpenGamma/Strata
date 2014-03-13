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
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

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
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {

    if (_latestSnapshot.containsKey(id)) {
      final Object value = _latestSnapshot.get(id).getValue(fieldName.getName());
      if (value != null) {
        return Result.success(value);
      } else {
        return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
      }
    } else if (_failedSubscriptions.containsKey(id)) {
      return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
    } else {
      _liveDataManager.makeSubscriptionRequest(
          new LiveDataManager.SubscriptionRequest<>(this, LiveDataManager.RequestType.SUBSCRIBE, id));
      return Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}/{}", id, fieldName);
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
