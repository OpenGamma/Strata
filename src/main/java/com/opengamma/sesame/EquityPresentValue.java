/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FunctionResultGenerator.failure;
import static com.opengamma.util.result.FunctionResultGenerator.propagateFailure;
import static com.opengamma.util.result.FunctionResultGenerator.success;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.FunctionResult;

public class EquityPresentValue implements EquityPresentValueFn {

  private final MarketDataFn _marketDataFn;

  public EquityPresentValue(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  @Override
  public FunctionResult<Double> presentValue(EquitySecurity security) {

    MarketDataRequirement requirement = MarketDataRequirementFactory.of(security,
                                                                        MarketDataRequirementNames.MARKET_VALUE);
    FunctionResult<MarketDataValues> result = _marketDataFn.requestData(requirement);

    if (result.getStatus().isResultAvailable()) {
      MarketDataValues marketDataValues = result.getResult();
      if (marketDataValues.getStatus(requirement) == MarketDataStatus.AVAILABLE) {
        return success((Double) marketDataValues.getOnlyValue());
      } else {
        return failure(FailureStatus.MISSING_DATA, "Market data was not available");
      }
    } else {
      return propagateFailure(result);
    }
  }
}
