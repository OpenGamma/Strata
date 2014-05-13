/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static com.opengamma.livedata.permission.PermissionUtils.LIVE_DATA_PERMISSION_FIELD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.fudgemsg.FudgeField;
import org.fudgemsg.FudgeMsg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.auth.AuthUtils;

/**
 * Represents a set of market data fields and the permissions
 * required for any user who wishes to view them.
 */
public class LiveDataUpdate {

  /**
   * The map of market data fields to values.
   */
  private final ImmutableMap<FieldName, Object> _fields;

  /**
   * The set of permissions required to see the market data.
   */
  private final ImmutableSet<Permission> _requiredPermissions;

  /**
   * Create a new instance for the specified fields and permissions.
   *
   * @param fields  the map of market data fields to values, not null
   * @param requiredPermissions  the set of permissions required to see
   * the market data, not null
   */
  public LiveDataUpdate(Map<FieldName, Object> fields, Set<Permission> requiredPermissions) {
    // ImmutableMap/Set take care of the null checking
    _fields = ImmutableMap.copyOf(fields);
    _requiredPermissions = ImmutableSet.copyOf(requiredPermissions);
  }

  /**
   * Creates an instance from a Fudge message, separating
   * the permissions from the rest of the market data fields.
   *
   * @param updatedValues the market values, not null
   * @return a new instance holding the market data values
   */
  public static LiveDataUpdate fromFudge(FudgeMsg updatedValues) {
    ArgumentChecker.notNull(updatedValues, "updatedValues");
    Map<FieldName, Object> converted = new HashMap<>();
    Set<String> permissions = new HashSet<>();
    for (FudgeField field : updatedValues) {
      if (field.getName().equals(LIVE_DATA_PERMISSION_FIELD)) {
        permissions.add((String) field.getValue());
      } else {
        converted.put(FieldName.of(field.getName()), field.getValue());
      }
    }
    Set<Permission> requiredPermissions = AuthUtils.getPermissionResolver().resolvePermissions(permissions);
    return new LiveDataUpdate(converted, requiredPermissions);
  }

  /**
   * Return the map of market data fields to values.
   *
   * @return the map of fields, not null
   */
  public ImmutableMap<FieldName, Object> getFields() {
    return _fields;
  }

  /**
   * Return the set of permissions required to view
   * the market data.
   *
   * @return the set of permissions, not null
   */
  public ImmutableSet<Permission> getRequiredPermissions() {
    return _requiredPermissions;
  }

}
