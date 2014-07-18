/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.core.marketdatasnapshot.CurveKey;
import com.opengamma.core.marketdatasnapshot.CurveSnapshot;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.core.marketdatasnapshot.StructuredMarketDataSnapshot;
import com.opengamma.core.marketdatasnapshot.UnstructuredMarketDataSnapshot;
import com.opengamma.core.marketdatasnapshot.ValueSnapshot;
import com.opengamma.core.marketdatasnapshot.impl.ManageableUnstructuredMarketDataSnapshot;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Source of market data backed by a single snapshot in the database.
 * TODO needs to support a listener in case the snapshot is changed in the DB. presumably same mechanism as live data
 */
public class SnapshotMarketDataSource implements MarketDataSource {

  private static final Logger s_logger = LoggerFactory.getLogger(SnapshotMarketDataSource.class);
  
  private final UnstructuredMarketDataSnapshot _snapshot;

  /**
   * Creates a source backed by a single snapshot of data.
   *
   * @param snapshotSource the source of the data snapshots
   * @param snapshotId the ID of the snapshot backing this data source
   */
  public SnapshotMarketDataSource(MarketDataSnapshotSource snapshotSource, UniqueId snapshotId) {
    ArgumentChecker.notNull(snapshotSource, "snapshotSource");
    // TODO if ID is unversioned need to get the VC from the engine to ensure consistency across the cycle
    _snapshot = getFlattenedSnapshot((StructuredMarketDataSnapshot) snapshotSource.get(snapshotId));
  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    ValueSnapshot value = _snapshot.getValue(id, fieldName.getName());
    if (value == null) {
      return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
    }

    Object overrideValue = value.getOverrideValue();
    if (overrideValue != null) {
      return Result.success(overrideValue);
    }

    Object marketValue = value.getMarketValue();
    if (marketValue != null) {
      return Result.success(marketValue);
    }
    return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
  }
  
  private UnstructuredMarketDataSnapshot getFlattenedSnapshot(StructuredMarketDataSnapshot snapshot) {
    ManageableUnstructuredMarketDataSnapshot result = new ManageableUnstructuredMarketDataSnapshot(snapshot.getGlobalValues());
    for (Map.Entry<CurveKey, CurveSnapshot> curveEntry : snapshot.getCurves().entrySet()) {
      UnstructuredMarketDataSnapshot curveValues = curveEntry.getValue().getValues();
      for (ExternalIdBundle target : curveValues.getTargets()) {
        Map<String, ValueSnapshot> targetValues = curveValues.getTargetValues(target);
        for (Map.Entry<String, ValueSnapshot> targetValue : targetValues.entrySet()) {
          String valueName = targetValue.getKey();
          ValueSnapshot value = targetValue.getValue();
          ValueSnapshot existing = result.getValue(target, valueName);
          if (existing != null && !existing.getMarketValue().equals(value.getMarketValue())) {
            s_logger.warn("Conflicting values found when flattening snapshot {}: for target {}, '{}' has existing value '{}' and alternative value '{}' in curve '{}'. Using existing value.",
                snapshot.getUniqueId(), target, existing.getMarketValue(), value.getMarketValue(), curveEntry.getKey().getName());
          } else {
            result.putValue(target, valueName, value);
          }
        }
      }
      // TODO: surfaces etc
    }
    return result;
  }

  // TODO this might be expensive, would it be reliable enough to base equality on the snapshot ID?
  @Override
  public int hashCode() {
    return Objects.hash(_snapshot);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SnapshotMarketDataSource other = (SnapshotMarketDataSource) obj;
    return Objects.equals(this._snapshot, other._snapshot);
  }
}
