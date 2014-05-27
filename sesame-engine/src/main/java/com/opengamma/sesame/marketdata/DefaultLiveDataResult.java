/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.auth.AuthUtils;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A live data result holding the market data values for a ticker.
 * <p>
 * This class is immutable and thread-safe.
 */
final class DefaultLiveDataResult implements LiveDataResult {

  /**
   * The ticker this result is for.
   */
  private final ExternalIdBundle _ticker;
  /**
   * The permissions a user requires to permit them to view the market data values.
   */
  private final ImmutableSet<Permission> _requiredPermissions;
  /**
   * The market data values.
   */
  private final ImmutableMap<FieldName, Object> _fields;

  /**
   * Create a result.
   *
   * @param ticker  the ticker the result is for, not null
   * @param update  the update containing the market data, not null
   */
  public DefaultLiveDataResult(ExternalIdBundle ticker, LiveDataUpdate update) {
    this(ticker, update.getRequiredPermissions(), ArgumentChecker.notNull(update, "update").getFields());
  }

  // constructor used internally
  private DefaultLiveDataResult(ExternalIdBundle ticker,
                               Set<Permission> requiredPermissions,
                               Map<FieldName, Object> updated) {
    _ticker = ArgumentChecker.notNull(ticker, "ticker");
    _requiredPermissions = ImmutableSet.copyOf(requiredPermissions);
    _fields = ImmutableMap.copyOf(updated);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isPending() {
    return false;
  }

  @Override
  public LiveDataResult permissionCheck() {
    try {
      // Throws exception if not permitted
      AuthUtils.getSubject().checkPermissions(_requiredPermissions);
      return this;
    } catch (Exception ex) {
      return new PermissionDeniedLiveDataResult(_ticker, ex);
    }
  }

  @Override
  public Result<?> getValue(FieldName name) {
    return _fields.containsKey(name) ?
        Result.success(_fields.get(name)) :
        Result.failure(FailureStatus.MISSING_DATA, "Data is available for id: {}, but not for field: {}", _ticker, name);
  }

  @Override
  public LiveDataResult update(LiveDataUpdate updatedValues) {
    ArgumentChecker.notNull(updatedValues, "updatedValues");

    // Merge the data values
    Map<FieldName, Object> updated = new HashMap<>(_fields);
    updated.putAll(updatedValues.getFields());

    // Merge the permissions
    Set<Permission> permissions = Sets.union(_requiredPermissions, updatedValues.getRequiredPermissions());

    return new DefaultLiveDataResult(_ticker, permissions, updated);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return _ticker + "=" + _fields;
  }

}
