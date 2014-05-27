/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A live data result indicating that a requested piece of market data is pending.
 * <p>
 * This class is immutable and thread-safe.
 */
public class PendingLiveDataResult implements LiveDataResult {

  /**
   * The ticker this result is for.
   */
  private final ExternalIdBundle _ticker;
  /**
   * Failure result indicating that the data is pending.
   */
  private final Result<?> _result;

  /**
   * Create the result.
   *
   * @param ticker  the ticker this result is for, not null
   */
  public PendingLiveDataResult(ExternalIdBundle ticker) {
    _ticker = ArgumentChecker.notNull(ticker, "ticker");
    _result = Result.failure(FailureStatus.PENDING_DATA, "Awaiting data for {}", ticker);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isPending() {
    return true;
  }

  @Override
  public LiveDataResult permissionCheck() {
    return this;
  }

  @Override
  public Result<?> getValue(FieldName name) {
    return _result;
  }

  /**
   * Returns a new result based entirely on the {@code LiveDataUpdate}.
   *
   * @param updatedValues  the new values
   * @return a new result based entirely on the update
   */
  @Override
  public LiveDataResult update(LiveDataUpdate updatedValues) {
    return new DefaultLiveDataResult(_ticker, updatedValues);
  }
}
