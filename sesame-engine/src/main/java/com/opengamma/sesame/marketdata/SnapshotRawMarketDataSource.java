/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.marketdatasnapshot.CurveKey;
import com.opengamma.core.marketdatasnapshot.CurveSnapshot;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.core.marketdatasnapshot.StructuredMarketDataSnapshot;
import com.opengamma.core.marketdatasnapshot.UnstructuredMarketDataSnapshot;
import com.opengamma.core.marketdatasnapshot.ValueSnapshot;
import com.opengamma.core.marketdatasnapshot.impl.ManageableUnstructuredMarketDataSnapshot;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO needs to support a listener in case the snapshot is changed in the DB. presumably same mechanism as live data
 */
public class SnapshotRawMarketDataSource extends AbstractRawMarketDataSource {

  private static final Logger s_logger = LoggerFactory.getLogger(SnapshotRawMarketDataSource.class);
  
  private final UnstructuredMarketDataSnapshot _snapshot;
  
  // REVIEW jonathan 2014-02-27 -- this is only here because of the methods which take a Period, and these should be removed anyway
  private final ValuationTimeFn _valuationTimeFn;

  public SnapshotRawMarketDataSource(MarketDataSnapshotSource snapshotSource,
                                            UniqueId snapshotId,
                                            HistoricalTimeSeriesSource timeSeriesSource,
                                            ValuationTimeFn valuationTimeFn,
                                            String dataProvider,
                                            String dataSource) {
    super(timeSeriesSource, dataProvider, dataSource);
    ArgumentChecker.notNull(snapshotSource, "snapshotSource");
    _valuationTimeFn = ArgumentChecker.notNull(valuationTimeFn, "valuationTimeFn");
    // TODO if ID is unversioned need to get the VC from the engine to ensure consistency across the cycle
    _snapshot = getFlattenedSnapshot(snapshotSource.get(snapshotId));
  }

  @Override
  public MarketDataItem get(ExternalIdBundle idBundle, String dataField) {
    ValueSnapshot value = _snapshot.getValue(idBundle, dataField);
    if (value == null) {
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    }

    Object overrideValue = value.getOverrideValue();
    if (overrideValue != null) {
      return MarketDataItem.available(overrideValue);
    }

    Object marketValue = value.getMarketValue();
    if (marketValue != null) {
      return MarketDataItem.available(marketValue);
    }
    return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
  }

  @Override
  public LocalDateRange calculateDateRange(Period period) {
    LocalDate date = _valuationTimeFn.getDate();
    LocalDate start = date.minus(period);
    return LocalDateRange.of(start, date, true);
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
  
}
