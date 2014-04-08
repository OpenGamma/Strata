/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;

/**
 * Trade wrapper for interest rate future trades.
 */
public class InterestRateFutureTrade extends TradeWrapper {

  public InterestRateFutureTrade(Trade trade) {
    super(trade);
    if (!(trade.getSecurity() instanceof InterestRateFutureSecurity)) {
      throw new IllegalArgumentException("Invalid trade type " + trade.getSecurity().getClass());
    }
  }
}
