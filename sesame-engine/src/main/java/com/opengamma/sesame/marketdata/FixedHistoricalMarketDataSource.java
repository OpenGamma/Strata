/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Objects;

import javax.annotation.Nullable;

import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Source of historical market from a fixed point in time.
 */
public class FixedHistoricalMarketDataSource implements MarketDataSource {

  private final LocalDate _snapshotDate;
  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final String _dataSource;
  private final String _dataProvider;

  /**
   * @param timeSeriesSource source of time series of historical data
   * @param snapshotDate the data for which data should be returned
   * @param dataSource the name of the data source
   * @param dataProvider the name of the data provider
   */
  public FixedHistoricalMarketDataSource(HistoricalTimeSeriesSource timeSeriesSource,
                                         LocalDate snapshotDate,
                                         String dataSource,
                                         @Nullable String dataProvider) {
    _timeSeriesSource = ArgumentChecker.notNull(timeSeriesSource, "timeSeriesSource");
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = dataProvider;
    _snapshotDate = ArgumentChecker.notNull(snapshotDate, "snapshotDate");

  }

  @Override
  public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
    HistoricalTimeSeries hts =
        _timeSeriesSource.getHistoricalTimeSeries(id, _dataSource, _dataProvider, fieldName.getName(),
                                                  _snapshotDate, true, _snapshotDate, true);
    if (hts == null || hts.getTimeSeries().isEmpty()) {
      return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}/{}", id, fieldName, _snapshotDate);
    }
    Double value = hts.getTimeSeries().getValue(_snapshotDate);

    if (value == null) {
      return Result.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}/{}", id, fieldName, _snapshotDate);
    } else {
      return Result.success(value);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(_snapshotDate, _timeSeriesSource, _dataSource, _dataProvider);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FixedHistoricalMarketDataSource other = (FixedHistoricalMarketDataSource) obj;
    return
        Objects.equals(this._snapshotDate, other._snapshotDate) &&
        Objects.equals(this._timeSeriesSource, other._timeSeriesSource) &&
        Objects.equals(this._dataSource, other._dataSource) &&
        Objects.equals(this._dataProvider, other._dataProvider);
  }
}
