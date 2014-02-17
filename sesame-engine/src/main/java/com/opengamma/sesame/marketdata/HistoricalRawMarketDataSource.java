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
public class HistoricalRawMarketDataSource extends AbstractRawMarketDataSource {

  private static final Logger s_logger = LoggerFactory.getLogger(HistoricalRawMarketDataSource.class);

  private final LocalDate _snapshotDate;

  public HistoricalRawMarketDataSource(HistoricalTimeSeriesSource timeSeriesSource,
                                       LocalDate snapshotDate,
                                       String dataSource,
                                       String dataProvider) {
    super(timeSeriesSource, dataProvider, dataSource);
    _snapshotDate = ArgumentChecker.notNull(snapshotDate, "snapshotDate");
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
  public LocalDateRange calculateDateRange(Period period) {
    LocalDate start = _snapshotDate.minus(period);
    return LocalDateRange.of(start, _snapshotDate, true);
  }
}
