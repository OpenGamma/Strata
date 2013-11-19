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
import com.opengamma.sesame.MarketDataFunctionResult;
import com.opengamma.sesame.MarketDataProviderFunction;
import com.opengamma.sesame.StandardResultGenerator;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pairs;

/**
 * Attempts to provide market data by immediately looking it up in a {@link RawMarketDataSource}.
 */
public class EagerMarketDataProvider implements MarketDataProviderFunction {

  private final RawMarketDataSource _rawMarketDataSource;
  // TODO use a ConfigLookup / ConfigResolver instead
  private final ConfigSource _configSource;
  private final String _currencyMatrixName;

  public EagerMarketDataProvider(RawMarketDataSource rawMarketDataSource,
                                 ConfigSource configSource,
                                 String currencyMatrixName) {
    // TODO this raises an important point. if functions look up config in the constructor we won't be able to track it
    // prohibit it? inject proxies to config etc. that won't allow lookups until activated? that will catch any
    // problems the first time a function runs
    _currencyMatrixName = ArgumentChecker.notEmpty(currencyMatrixName, "currencyMatrixName");
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _rawMarketDataSource = ArgumentChecker.notNull(rawMarketDataSource, "rawMarketDataSource");
  }

  @Override
  public MarketDataFunctionResult requestData(MarketDataRequirement requirement) {
    return requestData(Collections.singleton(requirement));
  }

  @Override
  public MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements) {
    MarketDataResultBuilder resultBuilder = StandardResultGenerator.marketDataResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      MarketDataValue<?> value = getValue(requirement);
      if (value != null) {
        resultBuilder.foundData(requirement, Pairs.of(MarketDataStatus.AVAILABLE, value));
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
      Double spotRate = ccyReq.getSpotRate(currencyMatrix, _rawMarketDataSource);
      if (spotRate != null) {
        return new SingleMarketDataValue(spotRate);
      } else {
        return null;
      }
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawMarketDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField());
    }
    return null;
  }
}
