/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;

public class EquityPresentValue implements EquityPresentValueFunction {

  private final MarketDataProviderFunction _marketDataProviderFunction;

  public EquityPresentValue(MarketDataProviderFunction marketDataProviderFunction) {
    _marketDataProviderFunction = marketDataProviderFunction;
  }

  @Override
  public FunctionResult<Double> execute(EquitySecurity security) {

    MarketDataFunctionResult result = _marketDataProviderFunction.requestData(
        StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE));

    // todo remove the nasty cast
    return result.isFullyAvailable() ?
        result.generateSuccessResult((Double) result.getSingleMarketDataValue().getValue()) :
        result.<Double>generateFailureResult();
  }
}
