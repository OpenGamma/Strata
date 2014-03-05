/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
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
  public Result<Double> presentValue(Environment env, EquitySecurity security) {
    ExternalIdBundle securityId = security.getExternalIdBundle();
    MarketDataItem<Double> item = _marketDataFn.getMarketValue(env, securityId);

    if (item.isAvailable()) {
      return success(item.getValue());
    } else {
      return failure(FailureStatus.MISSING_DATA, "Market data was not available for {}", securityId);
    }
  }
}
