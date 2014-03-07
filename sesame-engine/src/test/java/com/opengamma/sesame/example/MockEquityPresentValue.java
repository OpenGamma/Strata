/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.util.result.Result;

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
    return _marketDataFn.getMarketValue(env, security.getExternalIdBundle());
  }

}
