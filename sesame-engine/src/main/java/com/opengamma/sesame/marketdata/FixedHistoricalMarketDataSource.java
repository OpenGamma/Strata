/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 * Useful for backing a {@link MarketDataFn} which needs historical data from a fixed point in time.
 */
public class FixedHistoricalMarketDataSource implements MarketDataSource {

  private static final Logger s_logger = LoggerFactory.getLogger(FixedHistoricalMarketDataSource.class);

  private final LocalDate _snapshotDate;
  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final String _dataSource;
  private final String _dataProvider;

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
      s_logger.info("No time-series for {}", id);
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
    }
    Double value = hts.getTimeSeries().getValue(_snapshotDate);
    if (value == null) {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data found for {}/{}", id, fieldName);
    } else {
      return ResultGenerator.success(value);
    }
  }
}
