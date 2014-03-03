/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Set;

import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 *
 */
public class HistoricalTimeSeriesMarketDataFn implements HistoricalMarketDataFn {

  private final CurrencyMatrix _currencyMatrix;
  private final RawHistoricalMarketDataSource _rawDataSource;

  public HistoricalTimeSeriesMarketDataFn(CurrencyMatrix currencyMatrix, RawHistoricalMarketDataSource rawDataSource) {
    _currencyMatrix = ArgumentChecker.notNull(currencyMatrix, "currencyMatrix");
    _rawDataSource = ArgumentChecker.notNull(rawDataSource, "rawDataSource");
  }

  @Override
  public Result<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange) {
    return requestData(Collections.singleton(requirement), dateRange);
  }

  @Override
  public Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange) {
    MarketDataSeriesResultBuilder resultBuilder = new MarketDataSeriesResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataItem item = getSeries(requirement, dateRange);
      if (item.isAvailable()) {
        resultBuilder.foundData(requirement, item);
      } else {
        resultBuilder.missingData(requirement, item.getStatus());
      }
    }
    return resultBuilder.build();
  }

  private MarketDataItem getSeries(MarketDataRequirement requirement, LocalDateRange dateRange) {
    if (requirement instanceof CurrencyPairMarketDataRequirement) {

      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      return ccyReq.getFxRateSeries(dateRange, _currencyMatrix, _rawDataSource);
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField(), dateRange);
    }
    return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
  }
}
