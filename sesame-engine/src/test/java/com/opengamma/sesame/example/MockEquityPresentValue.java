/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 * Function implementation that provides present value for equities.
 */
public class MockEquityPresentValue implements MockEquityPresentValueFn {

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;

  public MockEquityPresentValue(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<Double> presentValue(Environment env, EquitySecurity security) {
    MarketDataItem<Double> result = _marketDataFn.getMarketValue(env, security.getExternalIdBundle());

    if (result.isAvailable()) {
      Double value = result.getValue();
      return ResultGenerator.success(value);
    } else {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data for " + security.getExternalIdBundle());
    }
  }

}
