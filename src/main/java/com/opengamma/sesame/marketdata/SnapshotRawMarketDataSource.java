/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.core.marketdatasnapshot.UnstructuredMarketDataSnapshot;
import com.opengamma.core.marketdatasnapshot.ValueSnapshot;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO needs to support a listener in case the snapshot is changed in the DB. presumably same mechanism as live data
 */
/* package */ class SnapshotRawMarketDataSource extends AbstractRawMarketDataSource {

  private final UnstructuredMarketDataSnapshot _snapshot;
  private final ValuationTimeFn _valuationTimeFn;

  /* package */ SnapshotRawMarketDataSource(MarketDataSnapshotSource snapshotSource,
                                            UniqueId snapshotId,
                                            HistoricalTimeSeriesSource timeSeriesSource,
                                            ValuationTimeFn valuationTimeFn,
                                            String dataProvider,
                                            String dataSource) {
    super(timeSeriesSource, dataProvider, dataSource);
    _valuationTimeFn = ArgumentChecker.notNull(valuationTimeFn, "valuationTimeFn");
    // TODO if ID is unversioned need to get the VC from the engine to ensure consistency across the cycle
    _snapshot = ArgumentChecker.notNull(snapshotSource, "snapshotSource").get(snapshotId).getGlobalValues();
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
}
