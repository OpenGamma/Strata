/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for deliverable swap future trades.
 */
public class IRFutureOptionTrade extends TradeWrapper<IRFutureOptionSecurity> {

  /**
   * Constructs an interest rate future option trade wrapper.
   * @param trade a trade instance containing an interest rate future option.
   */
  public IRFutureOptionTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), IRFutureOptionSecurity.class);
  }
}
