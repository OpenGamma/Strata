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
 * A live data result indicating that a requested piece of market data is missing.
 * <p>
 * This class is immutable and thread-safe.
 */
final class MissingLiveDataResult implements LiveDataResult {

  /**
   * The ticker this result is for.
   */
  private final ExternalIdBundle _ticker;
  /**
   * Failure result indicating the reason why the data is missing
   */
  private final Result<?> _result;

  /**
   * Create the result.
   *
   * @param ticker  the ticker this result is for, not null
   * @param message  message indicating why the market data is missing, not null
   */
  public MissingLiveDataResult(ExternalIdBundle ticker, String message) {
    _ticker = ArgumentChecker.notNull(ticker, "ticker");
    _result = Result.failure(FailureStatus.MISSING_DATA, ArgumentChecker.notNull(message, "message"));
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isPending() {
    return false;
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
   * @param update  the new values
   * @return a new result based entirely on the update
   */
  @Override
  public LiveDataResult update(LiveDataUpdate update) {
    return new DefaultLiveDataResult(_ticker, update);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "MISSING_DATA:" + _result.getFailureMessage();
  }

}
