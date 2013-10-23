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
  private final ResultGenerator _resultContext;

  public EquityPresentValue(ResultGenerator resultContext,
                            MarketDataProvider marketDataProvider) {
    _marketDataProvider = marketDataProvider;
    _resultContext = resultContext;
  }

  public FunctionResult<Double> calculateEquityPresentValue(MarketDataContext marketDataContext, EquitySecurity security) {

    MarketDataFunctionResult result = _marketDataProvider.retrieveMarketData(
        marketDataContext, security, MarketDataRequirementNames.MARKET_VALUE);

    if (result.isFullyAvailable()) {
      MarketDataValue value = result.getSingleMarketDataValue();
      return _resultContext.generateSuccessResult(value.asDouble());
    } else {
      return _resultContext.generateFailureResult(result);
    }
  }

}
