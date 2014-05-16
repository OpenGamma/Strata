/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.equity;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.util.result.Result;

/**
 * Default function implementation that provides present value for equities.
 * <p>
 * This returns the market value as the present value.
 */
public class DefaultEquityPresentValueFn implements EquityPresentValueFn {

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;

  /**
   * Creates an instance.
   * 
   * @param marketDataFn  the market data function
   */
  public DefaultEquityPresentValueFn(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<Double> presentValue(Environment env, EquitySecurity security) {
    return _marketDataFn.getMarketValue(env, security.getExternalIdBundle());
  }

}
