/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function implementation that provides present value for equities.
 */
public class EquityPresentValue implements EquityPresentValueFn {

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;

  public EquityPresentValue(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<Double> presentValue(EquitySecurity security) {

    MarketDataRequirement requirement = MarketDataRequirementFactory.of(security,
                                                                        MarketDataRequirementNames.MARKET_VALUE);
    Result<MarketDataValues> result = _marketDataFn.requestData(requirement);

    if (result.getStatus().isResultAvailable()) {
      MarketDataValues marketDataValues = result.getValue();
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
