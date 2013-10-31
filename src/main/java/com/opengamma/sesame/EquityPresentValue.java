/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.StandardResultGenerator.failure;
import static com.opengamma.sesame.StandardResultGenerator.propagateFailure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;

public class EquityPresentValue implements EquityPresentValueFunction {

  private final MarketDataProviderFunction _marketDataProviderFunction;

  public EquityPresentValue(MarketDataProviderFunction marketDataProviderFunction) {
    _marketDataProviderFunction = marketDataProviderFunction;
  }

  @Override
  public FunctionResult<Double> execute(EquitySecurity security) {

    MarketDataRequirement requirement = StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE);
    MarketDataFunctionResult result = _marketDataProviderFunction.requestData(requirement);

    if (result.getStatus().isResultAvailable()) {
      if (result.getMarketDataState(requirement) == MarketDataStatus.AVAILABLE) {
        return success((Double) result.getSingleMarketDataValue().getValue());
      } else {
        return failure(FailureStatus.MISSING_DATA, "Market data was not available");
      }
    } else {
      return propagateFailure(result);
    }
  }
}
