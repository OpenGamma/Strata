/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Set;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.StandardResultGenerator;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * Attempts to provide market data by immediately looking it up in a {@link RawMarketDataSource}.
 */
public class EagerMarketDataProvider implements MarketDataProviderFunction {

  private final RawMarketDataSource _rawDataSource;
  // TODO use a ConfigLookup / ConfigResolver instead
  private final ConfigSource _configSource;
  private final String _currencyMatrixName;

  public EagerMarketDataProvider(RawMarketDataSource rawDataSource,
                                 ConfigSource configSource,
                                 String currencyMatrixName) {
    _currencyMatrixName = ArgumentChecker.notEmpty(currencyMatrixName, "currencyMatrixName");
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _rawDataSource = ArgumentChecker.notNull(rawDataSource, "rawMarketDataSource");
  }

  @Override
  public MarketDataSingleResult requestData(MarketDataRequirement requirement) {
    return requestData(Collections.singleton(requirement));
  }

  @Override
  public MarketDataSingleResult requestData(Set<MarketDataRequirement> requirements) {
    MarketDataResultBuilder resultBuilder = StandardResultGenerator.marketDataResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataValue<?> value = getValue(requirement);
      if (value != null) {
        resultBuilder.foundData(requirement, MarketDataItem.available(value));
      } else {
        resultBuilder.missingData(requirement);
      }
    }
    return resultBuilder.build();
  }

  private MarketDataValue<?> getValue(MarketDataRequirement requirement) {
    if (requirement instanceof CurrencyPairMarketDataRequirement) {

      // TODO THIS IS DEFINITELY WRONG but will work for now. don't use latest, don't use ConfigSource
      CurrencyMatrix currencyMatrix = _configSource.getLatestByName(CurrencyMatrix.class, _currencyMatrixName);
      if (currencyMatrix == null) {
        throw new IllegalArgumentException("No currency matrix found named " + _currencyMatrixName);
      }
      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      // TODO handle the null case - null / missing impl of MarketDataValue
      Double spotRate = ccyReq.getSpotRate(currencyMatrix, _rawDataSource);
      if (spotRate != null) {
        return new SingleMarketDataValue(spotRate);
      } else {
        return null;
      }
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField());
    }
    return null;
  }

  @Override
  public MarketDataSeriesResult requestData(MarketDataRequirement requirement, LocalDateRange dateRange) {
    return requestData(Collections.singleton(requirement), dateRange);
  }

  @Override
  public MarketDataSeriesResult requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange) {
    MarketDataResultBuilder resultBuilder = StandardResultGenerator.marketDataResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataValue<?> value = getSeries(requirement, dateRange);
      if (value != null) {
        resultBuilder.foundData(requirement, MarketDataItem.available(value));
      } else {
        resultBuilder.missingData(requirement);
      }
    }
    throw new UnsupportedOperationException();
    // TODO what here?
    //return resultBuilder.build();
  }

  private MarketDataValue<?> getSeries(MarketDataRequirement requirement, LocalDateRange dateRange) {
    if (requirement instanceof CurrencyPairMarketDataRequirement) {

      // TODO THIS IS DEFINITELY WRONG but will work for now. don't use latest, don't use ConfigSource
      CurrencyMatrix currencyMatrix = _configSource.getLatestByName(CurrencyMatrix.class, _currencyMatrixName);
      if (currencyMatrix == null) {
        throw new IllegalArgumentException("No currency matrix found named " + _currencyMatrixName);
      }
      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      LocalDateDoubleTimeSeries spotRateSeries = ccyReq.getSpotRateSeries(dateRange, currencyMatrix, _rawDataSource);
      if (spotRateSeries != null) {
        // TODO need a value class for series? or get rid of MarketDataValue altogether?
        //return new SingleMarketDataValue(spotRateSeries);
        throw new UnsupportedClassVersionError();
      } else {
        return null;
      }
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField(), dateRange);
    }
    return null;
  }
}
