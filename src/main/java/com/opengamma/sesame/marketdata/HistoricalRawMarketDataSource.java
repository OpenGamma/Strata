/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * Useful for backing a {@link MarketDataFn} which needs historical data from a fixed point in time.
 */
public class HistoricalRawMarketDataSource implements RawMarketDataSource {

  private static final Logger s_logger = LoggerFactory.getLogger(HistoricalRawMarketDataSource.class);

  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final LocalDate _snapshotDate;
  private final String _dataSource;
  private final String _dataProvider;

  public HistoricalRawMarketDataSource(HistoricalTimeSeriesSource timeSeriesSource,
                                       LocalDate snapshotDate,
                                       String dataSource,
                                       String dataProvider) {
    _timeSeriesSource = ArgumentChecker.notNull(timeSeriesSource, "timeSeriesSource");
    _snapshotDate = ArgumentChecker.notNull(snapshotDate, "snapshotDate");
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = ArgumentChecker.notEmpty(dataProvider, "dataProvider");
  }

  @Override
  public MarketDataItem get(ExternalIdBundle idBundle, String dataField) {
    HistoricalTimeSeries hts = _timeSeriesSource.getHistoricalTimeSeries(idBundle, _dataSource, _dataProvider, dataField,
                                                                         _snapshotDate, true, _snapshotDate, true);
    if (hts == null || hts.getTimeSeries().isEmpty()) {
      s_logger.info("No time-series for {}", idBundle);
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    }
    Double value = hts.getTimeSeries().getValue(_snapshotDate);
    if (value == null) {
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    } else {
      return MarketDataItem.available(value);
    }
  }

  @Override
  public MarketDataItem get(ExternalIdBundle idBundle, String dataField, LocalDateRange dateRange) {
    LocalDate startDate = dateRange.getStartDateInclusive();
    LocalDate endDate = dateRange.getEndDateInclusive();
    HistoricalTimeSeries hts = _timeSeriesSource.getHistoricalTimeSeries(idBundle, _dataSource, _dataProvider, dataField,
                                                                         startDate, true, endDate, true);
    if (hts == null || hts.getTimeSeries().isEmpty()) {
      s_logger.info("No time-series for {}", idBundle);
      return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
    } else {
      return MarketDataItem.available(hts.getTimeSeries());
    }
  }

  @Override
  public LocalDateRange calculateDateRange(Period period) {
    LocalDate start = _snapshotDate.minus(period);
    return LocalDateRange.of(start, _snapshotDate, true);
  }
}
