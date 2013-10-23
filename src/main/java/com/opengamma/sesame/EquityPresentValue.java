/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;

public class EquityPresentValue {

  private final MarketDataProvider _marketDataProvider;
  private final FunctionContext _resultContext;

  public EquityPresentValue(FunctionContext resultContext,
                            MarketDataProvider marketDataProvider) {
    _marketDataProvider = marketDataProvider;
    _resultContext = resultContext;
  }

  public FunctionResult<Double> calculateEquityPresentValue(FunctionContext context, EquitySecurity security) {

    MarketDataFunctionResult result = _marketDataProvider.retrieveMarketData(security, MarketDataRequirementNames.MARKET_VALUE);

    if (result.isFullyAvailable()) {
      MarketDataValue value = result.getSingleMarketDataValue();
      return _resultContext.generateSuccessResult(value.asDouble());
    } else {
      return _resultContext.generateFailureResult(result);
    }
  }

}
