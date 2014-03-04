/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
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

/**
 * Attempts to provide market data by immediately looking it up in a {@link RawMarketDataSource}.
 * @deprecated use {@link MarketDataFn2}
 */
@Deprecated
public class EagerMarketDataFn implements MarketDataFn {

  private final RawMarketDataSource _rawDataSource;
  private final CurrencyMatrix _currencyMatrix;

  public EagerMarketDataFn(CurrencyMatrix currencyMatrix, RawMarketDataSource rawDataSource) {
    _currencyMatrix = ArgumentChecker.notNull(currencyMatrix, "currencyMatrix");
    _rawDataSource = ArgumentChecker.notNull(rawDataSource, "rawDataSource");
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

      CurrencyPairMarketDataRequirement ccyReq = (CurrencyPairMarketDataRequirement) requirement;
      return ccyReq.getFxRate(_currencyMatrix, _rawDataSource);
    } else if (requirement instanceof CurveNodeMarketDataRequirement) {

      CurveNodeMarketDataRequirement nodeReq = (CurveNodeMarketDataRequirement) requirement;
      return _rawDataSource.get(ExternalIdBundle.of(nodeReq.getExternalId()), nodeReq.getDataField());
    }
    return MarketDataItem.missing(MarketDataStatus.UNAVAILABLE);
  }
}
