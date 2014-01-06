/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Set;

import org.threeten.bp.Period;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * Attempts to provide market data by immediately looking it up in a {@link RawMarketDataSource}.
 */
public class EagerMarketDataFn implements MarketDataFn {

  private final RawMarketDataSource _rawDataSource;
  // TODO use a ConfigLookup / ConfigResolver instead
  private final ConfigSource _configSource;
  private final String _currencyMatrixName;

  public EagerMarketDataFn(RawMarketDataSource rawDataSource, ConfigSource configSource, String currencyMatrixName) {
    _currencyMatrixName = ArgumentChecker.notEmpty(currencyMatrixName, "currencyMatrixName");
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _rawDataSource = ArgumentChecker.notNull(rawDataSource, "rawMarketDataSource");
  }

  @Override
  public Result<MarketDataValues> requestData(MarketDataRequirement requirement) {
    return requestData(Collections.singleton(requirement));
  }

  @Override
  public Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
    MarketDataValuesResultBuilder resultBuilder = new MarketDataValuesResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataItem item = getValue(requirement);
      if (item.isAvailable()) {
        resultBuilder.foundData(requirement, item);
      } else {
        resultBuilder.missingData(requirement, item.getStatus());
      }
    }
    return resultBuilder.build();
  }

  private MarketDataItem getValue(MarketDataRequirement requirement) {
    if (requirement instanceof CurrencyPairMarketDataRequirement) {

      // TODO THIS IS DEFINITELY WRONG but will work for now. don't use latest, don't use ConfigSource
      CurrencyMatrix currencyMatrix = _configSource.getLatestByName(CurrencyMatrix.class, _currencyMatrixName);
      if (currencyMatrix == null) {
        throw new IllegalArgumentException("No currency matrix found named " + _currencyMatrixName);
      }
      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      return ccyReq.getSpotRate(currencyMatrix, _rawDataSource);
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField());
    }
    return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
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

      // TODO THIS IS DEFINITELY WRONG but will work for now. don't use latest, don't use ConfigSource
      CurrencyMatrix currencyMatrix = _configSource.getLatestByName(CurrencyMatrix.class, _currencyMatrixName);
      if (currencyMatrix == null) {
        throw new IllegalArgumentException("No currency matrix found named " + _currencyMatrixName);
      }
      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      return ccyReq.getSpotRateSeries(dateRange, currencyMatrix, _rawDataSource);
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField(), dateRange);
    }
    return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
  }

  @Override
  public Result<MarketDataSeries> requestData(MarketDataRequirement requirement, Period seriesPeriod) {
    return requestData(requirement, _rawDataSource.calculateDateRange(seriesPeriod));
  }

  @Override
  public Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, Period seriesPeriod) {
    return requestData(requirements, _rawDataSource.calculateDateRange(seriesPeriod));
  }
}
