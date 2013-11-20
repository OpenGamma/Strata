/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Collections;
import java.util.Set;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.CurrencyPairMarketDataRequirement;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValue;
import com.opengamma.sesame.marketdata.RawMarketDataSeriesSource;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;
import com.opengamma.util.tuple.Pairs;

/**
 * TODO experimental implementation to see how it feels
 */
public class TimeSeriesProvider implements MarketDataSeriesProviderFunction {

  private final ConfigSource _configSource;
  private final String _currencyMatrixName;
  private final RawMarketDataSeriesSource _rawDataSource;

  public TimeSeriesProvider(RawMarketDataSeriesSource rawDataSource,
                            ConfigSource configSource,
                            String currencyMatrixName) {
    _rawDataSource = ArgumentChecker.notNull(rawDataSource, "rawDataSource");
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _currencyMatrixName = ArgumentChecker.notEmpty(currencyMatrixName, "currencyMatrixName");
  }

  @Override
  public MarketDataFunctionResult requestData(MarketDataRequirement requirement, LocalDateRange dateRange) {
    return requestData(Collections.singleton(requirement), dateRange);
  }

  @Override
  public MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange) {
    MarketDataResultBuilder resultBuilder = StandardResultGenerator.marketDataResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataValue<?> value = getValue(requirement, dateRange);
      if (value != null) {
        resultBuilder.foundData(requirement, Pairs.of(MarketDataStatus.AVAILABLE, value));
      } else {
        resultBuilder.missingData(requirement);
      }
    }
    return resultBuilder.build();
  }

  private MarketDataValue<?> getValue(MarketDataRequirement requirement, LocalDateRange dateRange) {
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
