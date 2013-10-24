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
  private final ResultGenerator _resultContext;

  public EquityPresentValue(ResultGenerator resultContext,
                            MarketDataProviderFunction marketDataProviderFunction) {
    _marketDataProviderFunction = marketDataProviderFunction;
    _resultContext = resultContext;
  }

  @Override
  public FunctionResult<Double> calculateEquityPresentValue(MarketDataContext marketDataContext,
                                                            EquitySecurity security) {

    MarketDataFunctionResult result = _marketDataProviderFunction.retrieveMarketData(
        marketDataContext, security, MarketDataRequirementNames.MARKET_VALUE);

    return result.isFullyAvailable() ?
        _resultContext.generateSuccessResult(result.getSingleMarketDataValue().asDouble()) :
        _resultContext.<Double>generateFailureResult(result);
  }
}
