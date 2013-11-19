/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.SingleMarketDataValue;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pairs;

/**
 *
 */
public class HistoricalMarketData implements MarketDataProviderFunction {

  private static final Logger s_logger = LoggerFactory.getLogger(HistoricalMarketData.class);

  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final LocalDate _snapshotDate;
  private final String _dataSource;
  private final String _dataProvider;
  private final MarketDataResolver _marketDataResolver;

  public HistoricalMarketData(HistoricalTimeSeriesSource timeSeriesSource,
                              MarketDataResolver marketDataResolver,
                              LocalDate snapshotDate,
                              String dataSource,
                              String dataProvider) {
    _marketDataResolver = marketDataResolver;
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = ArgumentChecker.notEmpty(dataProvider, "dataProvider");
    _timeSeriesSource = ArgumentChecker.notNull(timeSeriesSource, "timeSeriesSource");
    _snapshotDate = ArgumentChecker.notNull(snapshotDate, "snapshotDate");
  }

  @Override
  public MarketDataFunctionResult requestData(MarketDataRequirement requirement) {
    return requestData(Collections.singleton(requirement));
  }

  @Override
  public MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements) {
    MarketDataResultBuilder resultBuilder = StandardResultGenerator.marketDataResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      List<MarketDataResolver.Item> items = _marketDataResolver.resolve(requirement);
      for (MarketDataResolver.Item item : items) {
        String dataField = item.getDataField();
        ExternalIdBundle ids = item.getIds();
        HistoricalTimeSeries hts = _timeSeriesSource.getHistoricalTimeSeries(ids, _dataSource, _dataProvider, dataField,
                                                                             _snapshotDate, true, _snapshotDate, true);
        if (hts == null || hts.getTimeSeries().isEmpty()) {
          s_logger.info("No time-series for {}", ids);
          resultBuilder.missingData(requirement);
          continue;
        }
        Double value = hts.getTimeSeries().getValue(_snapshotDate);
        if (value != null) {
          resultBuilder.foundData(requirement, Pairs.of(MarketDataStatus.AVAILABLE, new SingleMarketDataValue(value)));
        } else {
          resultBuilder.missingData(requirement);
        }
      }
    }
    return resultBuilder.build();
  }
}
