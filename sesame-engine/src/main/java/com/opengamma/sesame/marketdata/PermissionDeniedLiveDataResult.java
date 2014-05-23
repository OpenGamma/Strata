/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.util.result.FailureStatus.PERMISSION_DENIED;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * LiveDataResult indicating that a requested piece of market
 * data is not available as the user is not permissioned for it.
 */
public class PermissionDeniedLiveDataResult implements LiveDataResult {

  /**
   * Failure result indicating the reason why permission is denied.
   */
  private final Result<?> _result;

  /**
   * Create a result, storing the exception that caused the failure and
   * generating a default message.
   *
   * @param ticker  the ticker the result is for, not null
   * @param ex  the exception that caused the permission denial, not null
   */
  public PermissionDeniedLiveDataResult(ExternalIdBundle ticker, Exception ex) {
    ArgumentChecker.notNull(ticker, "ticker");
    ArgumentChecker.notNull(ex, "ex");
    if (ex instanceof UnauthenticatedException) {
      _result = Result.failure(PERMISSION_DENIED, ex, "Permission denied, user authentication error: {}", message(ex));
    } else if (ex instanceof UnauthorizedException) {
      _result = Result.failure(PERMISSION_DENIED, ex, "Permission denied for market data: {}", ticker.getExternalIds());
    } else {
      _result = Result.failure(PERMISSION_DENIED, ex, "Permission denied, unexpected error: {}", message(ex));
    }
  }

  // enhance error message
  private static String message(Exception ex) {
    String msg = ex.getMessage();
    if (msg.startsWith("Permission denied: ")) {
      return msg.substring("Permission denied: ".length());
    }
    if (msg.startsWith("Permission denied ")) {
      return msg.substring("Permission denied ".length());
    }
    return msg;
  }

  /**
   * Create a result, storing the supplied message.
   *
   * @param message  the message explaining the failure, not null
   */
  public PermissionDeniedLiveDataResult(String message) {
    _result = Result.failure(PERMISSION_DENIED, ArgumentChecker.notNull(message, "message"));
  }

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
   * Returns the same instance. It is not possible to update a permission
   * denied message with a new value.
   *
   * @param updatedValues  the new values, ignored
   * @return the same PermissionDeniedLiveDataResult instance
   */
  @Override
  public LiveDataResult update(LiveDataUpdate updatedValues) {
    return this;
  }

}
