/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for interest rate future trades.
 */
public class InterestRateFutureTrade extends TradeWrapper<InterestRateFutureSecurity> {

  /**
   * Constructs an interest rate future trade wrapper.
   * @param trade a trade instance containing an interest rate future, not null.
   */
  public InterestRateFutureTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), InterestRateFutureSecurity.class);
  }
}
